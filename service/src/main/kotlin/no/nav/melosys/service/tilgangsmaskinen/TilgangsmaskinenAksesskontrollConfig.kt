package no.nav.melosys.service.tilgangsmaskinen

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.AksesskontrollImpl
import no.nav.melosys.service.tilgang.Aksesstype
import no.nav.melosys.service.tilgang.BrukertilgangKontroll
import no.nav.melosys.service.tilgang.RedigerbarKontroll
import no.nav.melosys.service.tilgang.Ressurs
import no.nav.melosys.sikkerhet.logging.AuditLogger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Konfigurerer tilgangskontroll med runtime feature toggle checking
 *
 * RuntimeDelegatingAksesskontroll sjekker feature toggle ved hver kall og delegerer til:
 * - Toggle PÅ (AKSESSKONTROLL_TILGANGSMASKINEN=true) -> TilgangsmaskinenAksesskontroll
 * - Toggle AV (default) -> AksesskontrollImpl (ABAC)
 */
@Configuration
class TilgangsmaskinenAksesskontrollConfig {

    companion object {
        private val log = LoggerFactory.getLogger(TilgangsmaskinenAksesskontrollConfig::class.java)
    }

    @Bean
    @Primary
    fun aksesskontroll(
        unleash: Unleash,
        auditLogger: AuditLogger,
        fagsakService: FagsakService,
        behandlingService: BehandlingService,
        redigerbarKontroll: RedigerbarKontroll,
        oppgaveService: OppgaveService,
        brukertilgangKontroll: BrukertilgangKontroll,
        tilgangsmaskinenService: TilgangsmaskinenService
    ): Aksesskontroll {

        log.info("Konfigurerer runtime-delegating aksesskontroll med feature-toggle")

        val tilgangsmaskinenAksesskontroll = TilgangsmaskinenAksesskontroll(
            auditLogger = auditLogger,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            tilgangsmaskinenService = tilgangsmaskinenService,
            redigerbarKontroll = redigerbarKontroll,
            oppgaveService = oppgaveService
        )

        val abacAksesskontroll = AksesskontrollImpl(
            auditLogger,
            fagsakService,
            behandlingService,
            brukertilgangKontroll,
            redigerbarKontroll,
            oppgaveService
        )

        // Returner runtime-delegating wrapper for å håndtere toggle
        return RuntimeDelegatingAksesskontroll(
            unleash = unleash,
            tilgangsmaskinenAksesskontroll = tilgangsmaskinenAksesskontroll,
            abacAksesskontroll = abacAksesskontroll
        )
    }

    /**
     * Runtime-delegating implementasjon av Aksesskontroll
     *
     * Sjekker feature toggle ved hvert kall og delegerer til riktig implementasjon.
     * Dette tillater runtime endring av toggle uten restart av applikasjon.
     *
     * TODO: Når toggle er fjernet, slettes denne klassen og vi returnerer bare tilgangsmaskinenAksesskontroll.
     */
    private class RuntimeDelegatingAksesskontroll(
        private val unleash: Unleash,
        private val tilgangsmaskinenAksesskontroll: TilgangsmaskinenAksesskontroll,
        private val abacAksesskontroll: AksesskontrollImpl
    ) : Aksesskontroll {

        /**
         * Velger implementasjon basert på feature toggle
         */
        private fun velgImplementasjon(): Aksesskontroll {
            val erTilgangsmaskinenAktivert = unleash.isEnabled(ToggleName.AKSESSKONTROLL_TILGANGSMASKINEN)

            return if (erTilgangsmaskinenAktivert) {
                tilgangsmaskinenAksesskontroll
            } else {
                abacAksesskontroll
            }
        }

        override fun auditAutoriserAktørID(aktørID: String, kontekst: String) {
            velgImplementasjon().auditAutoriserAktørID(aktørID, kontekst)
        }

        override fun auditAutoriser(behandlingID: Long, kontekst: String) {
            velgImplementasjon().auditAutoriser(behandlingID, kontekst)
        }

        override fun auditAutoriserSkriv(behandlingID: Long, kontekst: String) {
            velgImplementasjon().auditAutoriserSkriv(behandlingID, kontekst)
        }

        override fun auditAutoriserFolkeregisterIdent(ident: String, kontekst: String) {
            velgImplementasjon().auditAutoriserFolkeregisterIdent(ident, kontekst)
        }

        override fun auditAutoriserSakstilgang(fagsak: Fagsak, kontekst: String) {
            velgImplementasjon().auditAutoriserSakstilgang(fagsak, kontekst)
        }

        override fun autoriserSakstilgang(saksnummer: String) {
            velgImplementasjon().autoriserSakstilgang(saksnummer)
        }

        override fun autoriserSakstilgang(fagsak: Fagsak) {
            velgImplementasjon().autoriserSakstilgang(fagsak)
        }

        override fun autoriser(behandlingID: Long) {
            velgImplementasjon().autoriser(behandlingID)
        }

        override fun autoriser(behandlingID: Long, aksesstype: Aksesstype) {
            velgImplementasjon().autoriser(behandlingID, aksesstype)
        }

        override fun autoriserSkriv(behandlingID: Long) {
            velgImplementasjon().autoriserSkriv(behandlingID)
        }

        override fun autoriserSkrivOgTilordnet(behandlingID: Long) {
            velgImplementasjon().autoriserSkrivOgTilordnet(behandlingID)
        }

        override fun autoriserSkrivTilRessurs(behandlingID: Long, ressurs: Ressurs) {
            velgImplementasjon().autoriserSkrivTilRessurs(behandlingID, ressurs)
        }

        override fun autoriserFolkeregisterIdent(brukerID: String) {
            velgImplementasjon().autoriserFolkeregisterIdent(brukerID)
        }

        override fun behandlingKanRedigeresAvSaksbehandler(behandling: Behandling, saksbehandler: String): Boolean {
            return velgImplementasjon().behandlingKanRedigeresAvSaksbehandler(behandling, saksbehandler)
        }

        override fun behandlingKanRedigeresAvSaksbehandler(behandlingID: Long): Boolean {
            return velgImplementasjon().behandlingKanRedigeresAvSaksbehandler(behandlingID)
        }
    }
}
