package no.nav.melosys.saksflyt.steg.oppgave

import mu.KotlinLogging
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OpprettOppgave(private val oppgaveService: OppgaveService) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_OPPGAVE

    override fun utfør(prosessinstans: Prosessinstans) {
        // Digital søknad i NY-flyten kan under DB-låsen ha festet seg på en eksisterende sak (MELOSYS-8151).
        // Da er oppgaven allerede håndtert i attach-logikken (oppgave kun ved NY_VURDERING), som i
        // EKSISTERENDE-flyten som ikke har eget OPPRETT_OPPGAVE-steg — så dette steget hoppes over.
        if (prosessinstans.finnData(ProsessDataKey.DIGITAL_SØKNAD_ATTACHED_EKSISTERENDE, false)) {
            log.info { "Hopper over OPPRETT_OPPGAVE — digital søknad ble festet på eksisterende sak" }
            return
        }

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            prosessinstans.hentBehandling,
            prosessinstans.hentJournalpostID(),
            prosessinstans.hentBehandling.fagsak.finnBrukersAktørID(),
            prosessinstans.hentSaksbehandlerHvisTilordnes(),
            prosessinstans.hentBehandling.fagsak.finnVirksomhetsOrgnr()
        )
    }
}
