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
        val opprettÅrsavregningUtenBbehandling = prosessinstans.getData(ProsessDataKey.ÅRSAVREGNING_STEG_KJØRT_UTEN_BEHANDLING, Boolean::class.java)
        if (opprettÅrsavregningUtenBbehandling != null && opprettÅrsavregningUtenBbehandling) {
            log.warn { "Årsavregning er opprettet uten behandling for sak ${prosessinstans.getData(ProsessDataKey.SAKSNUMMER)}" }
            return
        }

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            prosessinstans.behandling,
            prosessinstans.hentJournalpostID(),
            prosessinstans.behandling.fagsak.finnBrukersAktørID(),
            prosessinstans.hentSaksbehandlerHvisTilordnes(),
            prosessinstans.behandling.fagsak.finnVirksomhetsOrgnr()
        )
    }
}
