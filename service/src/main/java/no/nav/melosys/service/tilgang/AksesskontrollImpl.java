package no.nav.melosys.service.tilgang;

import no.nav.melosys.config.MDCOperations;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.logging.AuditEvent;
import no.nav.melosys.sikkerhet.logging.AuditEventType;
import no.nav.melosys.sikkerhet.logging.AuditLogger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static no.nav.melosys.service.tilgang.Aksesstype.LES;
import static no.nav.melosys.service.tilgang.Aksesstype.SKRIV;

@Transactional(readOnly = true)
public class AksesskontrollImpl implements Aksesskontroll {

    private final AuditLogger auditLogger;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BrukertilgangKontroll brukertilgangKontroll;
    private final RedigerbarKontroll redigerbarKontroll;
    private final OppgaveService oppgaveService;

    public AksesskontrollImpl(AuditLogger auditLogger, FagsakService fagsakService,
                              BehandlingService behandlingService,
                              BrukertilgangKontroll brukertilgangKontroll,
                              RedigerbarKontroll redigerbarKontroll,
                              OppgaveService oppgaveService) {
        this.auditLogger = auditLogger;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.brukertilgangKontroll = brukertilgangKontroll;
        this.redigerbarKontroll = redigerbarKontroll;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public void auditAutoriserAktørID(String aktørID, String kontekst) {
        logAudit(AuditEventType.READ, aktørID, kontekst);
        brukertilgangKontroll.validerTilgangTilAktørID(aktørID);
    }

    @Override
    public void auditAutoriser(long behandlingID, String kontekst) {
        var behandling = behandlingService.hentBehandling(behandlingID);
        String aktørID = behandling.getFagsak().finnBrukersAktørID();
        if (aktørID != null) {
            logAudit(AuditEventType.READ, aktørID, kontekst);
            brukertilgangKontroll.validerTilgangTilAktørID(aktørID);
        }
    }

    @Override
    @Transactional
    public void auditAutoriserSkriv(long behandlingID, String kontekst) {
        var behandling = behandlingService.hentBehandling(behandlingID);
        String aktørID = behandling.getFagsak().finnBrukersAktørID();
        if (aktørID != null) {
            logAudit(AuditEventType.UPDATE, aktørID, kontekst);
            autoriser(behandling, SKRIV, Ressurs.UKJENT, false);
        }
    }

    @Override
    public void auditAutoriserFolkeregisterIdent(String fnr, String kontekst) {
        logAudit(AuditEventType.READ, fnr, kontekst);
        brukertilgangKontroll.validerTilgangTilFolkeregisterIdent(fnr);
    }

    @Override
    public void auditAutoriserSakstilgang(Fagsak fagsak, String kontekst) {
        Optional.ofNullable(fagsak.finnBrukersAktørID()).ifPresent(aktørID -> auditAutoriserAktørID(aktørID, kontekst));
    }

    private void logAudit(AuditEventType eventType, String personIdent, String message) {
        AuditEvent auditEvent = new AuditEvent(eventType, SubjectHandler.getInstance().getUserID(), personIdent, message, MDCOperations.getCorrelationId());
        auditLogger.log(auditEvent);
    }

    @Override
    public void autoriserSakstilgang(String saksnummer) {
        autoriserSakstilgang(fagsakService.hentFagsak(saksnummer));
    }

    @Override
    public void autoriserSakstilgang(Fagsak fagsak) {
        Optional.ofNullable(fagsak.finnBrukersAktørID()).ifPresent(brukertilgangKontroll::validerTilgangTilAktørID);
    }

    @Override
    public void autoriser(long behandlingID) {
        autoriser(behandlingID, LES);
    }

    @Override
    public void autoriser(long behandlingID, Aksesstype aksesstype) {
        autoriser(behandlingService.hentBehandling(behandlingID), aksesstype, Ressurs.UKJENT, false);
    }

    @Override
    public void autoriserSkriv(long behandlingID) {
        autoriser(behandlingService.hentBehandling(behandlingID), SKRIV, Ressurs.UKJENT, false);
    }

    public void autoriserSkrivOgTilordnet(long behandlingID) {
        autoriser(behandlingService.hentBehandling(behandlingID), SKRIV, Ressurs.UKJENT, true);
    }

    @Override
    @Transactional
    public void autoriserSkrivTilRessurs(long behandlingID, Ressurs ressurs) {
        autoriser(behandlingService.hentBehandling(behandlingID), SKRIV, ressurs, false);
    }

    @Override
    public void autoriserFolkeregisterIdent(String folkeregisterIdent) {
        brukertilgangKontroll.validerTilgangTilFolkeregisterIdent(folkeregisterIdent);
    }

    @Override
    public boolean behandlingKanRedigeresAvSaksbehandler(Behandling behandling, String saksbehandler) {
        return redigerbarKontroll.behandlingErRedigerbar(behandling)
            && sakErTilordnetSaksbehandler(behandling.getId(), saksbehandler);
    }

    @Override
    public boolean behandlingKanRedigeresAvSaksbehandler(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        return redigerbarKontroll.behandlingErRedigerbar(behandling)
            && sakErTilordnetSaksbehandler(behandling.getId(), SubjectHandler.getInstance().getUserID());
    }

    private void autoriser(Behandling behandling, Aksesstype aksesstype, Ressurs ressurs, boolean validerTilordnet) {
        Optional.ofNullable(behandling.getFagsak().finnBrukersAktørID()).ifPresent(brukertilgangKontroll::validerTilgangTilAktørID);

        if (aksesstype == SKRIV) {
            if (validerTilordnet) {
                sjekkTilordnetSaksbehandler(behandling, SubjectHandler.getInstance().getUserID());
            }

            redigerbarKontroll.sjekkRessursRedigerbar(behandling, ressurs);
        }
    }

    private void sjekkTilordnetSaksbehandler(Behandling behandling, String saksbehandler) {
        if (!sakErTilordnetSaksbehandler(behandling.getId(), saksbehandler)) {
            throw new FunksjonellException(
                "Forsøk på å endre behandling med id %s som er ikke-redigerbar eller ikke er tilordnet %s".formatted(behandling.getId(), saksbehandler)
            );
        }
    }

    private boolean sakErTilordnetSaksbehandler(Long behandlingID, String saksbehandler) {
        return oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling(saksbehandler, behandlingID);
    }
}
