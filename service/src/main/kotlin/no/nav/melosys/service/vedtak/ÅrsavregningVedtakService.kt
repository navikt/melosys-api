package no.nav.melosys.service.vedtak

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ÅrsavregningVedtakService(
    val prosessinstansService: ProsessinstansService,
    val behandlingService: BehandlingService,
    val behandlingsresultatService: BehandlingsresultatService,
    val oppgaveService: OppgaveService,
    val dokgenService: DokgenService
) : FattVedtakInterface {
    private val log = KotlinLogging.logger { }

    override fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
        valider(behandling)

        val behandlingID = behandling.id
        log.info("Fatter årsavregningsvedtak for sak: ${behandling.fagsak.saksnummer} behandling: $behandlingID")

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw FunksjonellException("Det finnes allerede en vedtak-prosess for behandling $behandlingID")
        }

        oppdaterBehandlingsresultat(behandlingID, request)
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK)
        prosessinstansService.opprettProsessinstansIverksettVedtakÅrsavregning(behandling)
        dokgenService.produserOgDistribuerBrev(behandlingID, lagVedtaksbrev(request))
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }

    private fun valider(behandling: Behandling) {
        when {
            behandling.type != Behandlingstyper.ÅRSAVREGNING -> throw FunksjonellException("Kan kun fatte vedtak for Behandlingstype: Årsavregning")
        }

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

    private fun oppdaterBehandlingsresultat(behandlingID: Long, request: FattVedtakRequest): Behandlingsresultat {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        behandlingsresultat.type = request.behandlingsresultatTypeKode
        behandlingsresultat.settVedtakMetadata(request.vedtakstype, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
        behandlingsresultat.nyVurderingBakgrunn = request.nyVurderingBakgrunn
        behandlingsresultat.begrunnelseFritekst = request.begrunnelseFritekst
        behandlingsresultat.innledningFritekst = request.innledningFritekst
        behandlingsresultat.trygdeavgiftFritekst = request.trygdeavgiftFritekst
        behandlingsresultat.fastsattAvLand = Land_iso2.NO

        return behandlingsresultatService.lagre(behandlingsresultat)
    }
}
