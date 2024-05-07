package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OpprettAvgiftsoppgave(
    private val behandlingsresultatService: BehandlingsresultatService,
    private val oppgaveService: OppgaveService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg = ProsessSteg.OPPRETT_AVGIFTSOPPGAVE

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val behandlingID = behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        if (!behandlingsresultat.erAvslag() && !behandling.fagsak.erSakstypeFtrl()) {
            val lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode()
            if (!lovvalgsperiode.erArtikkel11() && !lovvalgsperiode.erArtikkel13()) {
                oppgaveService.opprettOppgave(lagOppgaveTilTrygdeavgift(behandling))
            }
        }
    }

    companion object {
        private const val FRIST_AVGIFTSVURDERING_MD: Long = 1
        const val AVGIFTSVURDERING_BESKRIVELSE = "Vurderes for innregistrering i Avgiftssystemet"
        private fun lagOppgaveTilTrygdeavgift(behandling: Behandling): Oppgave {
            val fagsak = behandling.fagsak
            return Oppgave.Builder()
                .setTema(Tema.TRY).setOppgavetype(Oppgavetyper.VUR)
                .setJournalpostId(behandling.initierendeJournalpostId)
                .setBehandlesAvApplikasjon(Fagsystem.INTET)
                .setAktørId(fagsak.hentBrukersAktørID())
                .setBeskrivelse(AVGIFTSVURDERING_BESKRIVELSE)
                .setFristFerdigstillelse(LocalDate.now().plusMonths(FRIST_AVGIFTSVURDERING_MD))
                .setSaksnummer(fagsak.saksnummer)
                .build()
        }
    }
}
