package no.nav.melosys.service.tilgangsmaskinen

import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Aksesstype
import no.nav.melosys.service.tilgang.RedigerbarKontroll
import no.nav.melosys.service.tilgang.Ressurs
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.logging.AuditEvent
import no.nav.melosys.sikkerhet.logging.AuditEventType
import no.nav.melosys.sikkerhet.logging.AuditLogger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Tilgangsmaskinen-basert implementasjon av Aksesskontroll
 *
 * Erstatter ABAC-implementasjonen ved å bruke TilgangsmaskinenService
 * for tilgangskontroll, men beholder all annen logikk (audit, skrivetilgang, etc.)
 */

private val log = KotlinLogging.logger { }

@Component("tilgangsmaskineAksesskontroll")
@Transactional(readOnly = true)
class TilgangsmaskinenAksesskontroll(
    private val auditLogger: AuditLogger,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilgangsmaskinenService: TilgangsmaskinenService,
    private val redigerbarKontroll: RedigerbarKontroll,
    private val oppgaveService: OppgaveService
) : Aksesskontroll {

    companion object {
        private const val IKKE_TILGANG = "Tilgangsmaskinen: Brukeren har ikke tilgang til ressurs"
    }

    override fun auditAutoriserAktørID(aktørID: String, kontekst: String) {
        logAudit(AuditEventType.READ, aktørID, kontekst)
        validerTilgangTilAktørID(aktørID)
    }

    override fun auditAutoriser(behandlingID: Long, kontekst: String) {
        val behandling = behandlingService.hentBehandling(behandlingID)
        val aktørID = behandling.fagsak.finnBrukersAktørID()
        if (aktørID != null) {
            logAudit(AuditEventType.READ, aktørID, kontekst)
            validerTilgangTilAktørID(aktørID)
        }
    }

    override fun auditAutoriserSkriv(behandlingID: Long, kontekst: String) {
        val behandling = behandlingService.hentBehandling(behandlingID)
        val aktørID = behandling.fagsak.finnBrukersAktørID()
        if (aktørID != null) {
            logAudit(AuditEventType.UPDATE, aktørID, kontekst)
            autoriser(behandling, Aksesstype.SKRIV, Ressurs.UKJENT, false)
        }
    }

    override fun auditAutoriserFolkeregisterIdent(ident: String, kontekst: String) {
        logAudit(AuditEventType.READ, ident, kontekst)
        validerTilgangTilFolkeregisterIdent(ident)
    }

    override fun auditAutoriserSakstilgang(fagsak: Fagsak, kontekst: String) {
        fagsak.finnBrukersAktørID()?.let { aktørID ->
            auditAutoriserAktørID(aktørID, kontekst)
        }
    }

    private fun logAudit(eventType: AuditEventType, personIdent: String, message: String) {
        val auditEvent = AuditEvent(
            eventType,
            SubjectHandler.getInstance().userID,
            personIdent,
            message,
            MDCOperations.getCorrelationId()
        )
        auditLogger.log(auditEvent)
    }

    override fun autoriserSakstilgang(saksnummer: String) {
        autoriserSakstilgang(fagsakService.hentFagsak(saksnummer))
    }

    override fun autoriserSakstilgang(fagsak: Fagsak) {
        fagsak.finnBrukersAktørID()?.let { aktørID ->
            validerTilgangTilAktørID(aktørID)
        }
    }

    override fun autoriser(behandlingID: Long) {
        autoriser(behandlingID, Aksesstype.LES)
    }

    override fun autoriser(behandlingID: Long, aksesstype: Aksesstype) {
        autoriser(behandlingService.hentBehandling(behandlingID), aksesstype, Ressurs.UKJENT, false)
    }

    override fun autoriserSkriv(behandlingID: Long) {
        autoriser(behandlingService.hentBehandling(behandlingID), Aksesstype.SKRIV, Ressurs.UKJENT, false)
    }

    override fun autoriserSkrivOgTilordnet(behandlingID: Long) {
        autoriser(behandlingService.hentBehandling(behandlingID), Aksesstype.SKRIV, Ressurs.UKJENT, true)
    }

    override fun autoriserSkrivTilRessurs(behandlingID: Long, ressurs: Ressurs) {
        autoriser(behandlingService.hentBehandling(behandlingID), Aksesstype.SKRIV, ressurs, false)
    }

    override fun autoriserFolkeregisterIdent(brukerID: String) {
        validerTilgangTilFolkeregisterIdent(brukerID)
    }

    override fun behandlingKanRedigeresAvSaksbehandler(behandling: Behandling, saksbehandler: String): Boolean {
        return redigerbarKontroll.behandlingErRedigerbar(behandling) &&
                sakErTilordnetSaksbehandler(behandling.id, saksbehandler)
    }

    override fun behandlingKanRedigeresAvSaksbehandler(behandlingID: Long): Boolean {
        val behandling = behandlingService.hentBehandling(behandlingID)
        return redigerbarKontroll.behandlingErRedigerbar(behandling) &&
                sakErTilordnetSaksbehandler(behandling.id, SubjectHandler.getInstance().userID)
    }

    private fun autoriser(behandling: Behandling, aksesstype: Aksesstype, ressurs: Ressurs, validerTilordnet: Boolean) {
        behandling.fagsak.finnBrukersAktørID()?.let { aktørID ->
            validerTilgangTilAktørID(aktørID)
        }

        if (aksesstype == Aksesstype.SKRIV) {
            if (validerTilordnet) {
                sjekkTilordnetSaksbehandler(behandling, SubjectHandler.getInstance().userID)
            }

            redigerbarKontroll.sjekkRessursRedigerbar(behandling, ressurs)
        }
    }

    private fun sjekkTilordnetSaksbehandler(behandling: Behandling, saksbehandler: String) {
        if (!sakErTilordnetSaksbehandler(behandling.id, saksbehandler)) {
            throw FunksjonellException(
                "Forsøk på å endre behandling med id ${behandling.id} som er ikke-redigerbar eller ikke er tilordnet $saksbehandler"
            )
        }
    }

    private fun sakErTilordnetSaksbehandler(behandlingID: Long, saksbehandler: String): Boolean {
        return oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling(saksbehandler, behandlingID)
    }

    /**
     * Validerer tilgang til aktørID via Tilgangsmaskinen
     * Erstatter ABAC-basert tilgangskontroll
     */
    private fun validerTilgangTilAktørID(aktørID: String) {
        log.debug("Validerer tilgang til aktørID via Tilgangsmaskinen")

        val harTilgang = try {
            tilgangsmaskinenService.sjekkTilgangTilAktørId(aktørID)
        } catch (ex: Exception) {
            log.error("Feil ved tilgangskontroll for aktørID: $aktørID", ex)
            throw SikkerhetsbegrensningException(IKKE_TILGANG)
        }

        if (!harTilgang) {
            log.warn("Tilgang nektet for aktørID: $aktørID")
            throw SikkerhetsbegrensningException(IKKE_TILGANG)
        }
    }

    /**
     * Validerer tilgang til fødselsnummer/d-nummer via Tilgangsmaskinen
     * Erstatter ABAC-basert tilgangskontroll
     */
    private fun validerTilgangTilFolkeregisterIdent(fnr: String) {
        log.debug("Validerer tilgang til fnr via Tilgangsmaskinen")

        val harTilgang = try {
            tilgangsmaskinenService.sjekkTilgangTilFnr(fnr)
        } catch (ex: Exception) {
            log.error("Feil ved tilgangskontroll for fnr", ex)
            throw SikkerhetsbegrensningException(IKKE_TILGANG)
        }

        if (!harTilgang) {
            log.warn("Tilgang nektet for fnr")
            throw SikkerhetsbegrensningException(IKKE_TILGANG)
        }
    }
}
