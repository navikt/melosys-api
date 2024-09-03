package no.nav.melosys.service.vedtak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// TODO 6793
@Service
class ÅrsavregningVedtakService(
    val prosessinstansService: ProsessinstansService,
    val behandlingService: BehandlingService,
    val oppgaveService: OppgaveService,
    val dokgenService: DokgenService
) {
    private val log = LoggerFactory.getLogger(FtrlVedtakService::class.java)

    fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
        val behandlingID = behandling.id
        log.info("Fatter vedtak for (FTRL) sak: ${behandling.fagsak.saksnummer} behandling: $behandlingID")
        // inkluder evt validering av request
        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw FunksjonellException("Det finnes allerede en vedtak-prosess for behandling $behandlingID")
        }

        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK)
        prosessinstansService.opprettProsessinstansIverksettVedtakÅrsavregning(behandling)
        dokgenService.produserOgDistribuerBrev(behandlingID, lagVedtaksbrev(request))
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }

    private fun lagVedtaksbrev(request: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {

            produserbardokument = Produserbaredokumenter.AARSAVREGNING_VEDTAKSBREV
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = request.kopiMottakere
            bestillersId = request.bestillersId
            innledningFritekst = request.innledningFritekst
            begrunnelseFritekst = request.begrunnelseFritekst
        }
}
