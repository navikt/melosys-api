package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AvslagService(
    val behandlingService: BehandlingService,
    val behandlingsresultatService: BehandlingsresultatService,
    val dokgenService: DokgenService,
    val oppgaveService: OppgaveService,
    val fagsakService: FagsakService,
) {
    val FRIST_KLAGE_UKER = 6L

    @Transactional
    fun avslåPgaManglendeOpplysninger(behandlingID: Long, fritekst: String, bestillersId: String) {
        val behandling = behandlingService.hentBehandling(behandlingID)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        behandlingsresultat.apply {
            type = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
            fastsattAvLand = Land_iso2.NO
            this.settVedtakMetadata(LocalDate.now().plusWeeks(FRIST_KLAGE_UKER))
        }
        behandlingsresultat.medlemskapsperioder.clear()
        behandlingsresultatService.lagre(behandlingsresultat)

        dokgenService.produserOgDistribuerBrev(behandlingID, lagBrevbestillingDto(fritekst, bestillersId))
        fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, Saksstatuser.LOVVALG_AVKLART)
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }

    private fun lagBrevbestillingDto(fritekst: String, bestillersId: String): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER
            mottaker = Mottakerroller.BRUKER
            this.bestillersId = bestillersId
            this.fritekst = fritekst
        }
}
