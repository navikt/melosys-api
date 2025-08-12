package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component

@Component
class OpprettOppgave(private val oppgaveService: OppgaveService) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_OPPGAVE

    override fun utfør(prosessinstans: Prosessinstans) {
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            prosessinstans.hentBehandling,
            prosessinstans.hentJournalpostID(),
            prosessinstans.hentBehandling.fagsak.finnBrukersAktørID(),
            prosessinstans.hentSaksbehandlerHvisTilordnes(),
            prosessinstans.hentBehandling.fagsak.finnVirksomhetsOrgnr()
        )
    }
}
