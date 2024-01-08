package no.nav.melosys.service.vedtak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FtrlVedtakService(
    val behandlingsresultatService: BehandlingsresultatService,
    val behandlingService: BehandlingService,
    val prosessinstansService: ProsessinstansService,
    val oppgaveService: OppgaveService,
    val dokgenService: DokgenService,
    val vilkaarsresultatService: VilkaarsresultatService
) {
    private val log = LoggerFactory.getLogger(FtrlVedtakService::class.java)

    fun fattVedtak(behandling: Behandling, request: FattVedtakRequest) {
        val behandlingID = behandling.id
        log.info("Fatter vedtak for (FTRL) sak: ${behandling.fagsak.saksnummer} behandling: $behandlingID")

        oppdaterBehandlingsresultat(behandlingID, request)
        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw FunksjonellException("Det finnes allerede en vedtak-prosess for behandling $behandlingID")
        }

        behandling.fagsak.status =
            if (request.behandlingsresultatTypeKode == Behandlingsresultattyper.OPPHØRT) Saksstatuser.OPPHØRT else Saksstatuser.MEDLEMSKAP_AVKLART

        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK)
        prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(behandling, request.tilVedtakRequest())
        dokgenService.produserOgDistribuerBrev(behandlingID, lagBrevbestilling(request))
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.fagsak.saksnummer)
    }

    private fun lagBrevbestilling(request: FattVedtakRequest): BrevbestillingDto {
        if (request.behandlingsresultatTypeKode == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return lagAvslagMangledeOpplysningerBrevbestilling(request)
        }
        if (request.behandlingsresultatTypeKode in listOf(Behandlingsresultattyper.OPPHØRT, Behandlingsresultattyper.DELVIS_OPPHØRT)) {
            return lagVedtakOpphørtMedlemskapBrevbestilling(request)
        }
        return lagInnvilgelseFolketrygdloven(request)
    }


    private fun lagVedtakOpphørtMedlemskapBrevbestilling(request: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = request.kopiMottakere
            begrunnelseFritekst = request.begrunnelseFritekst
            bestillersId = request.bestillersId
        }


    private fun lagAvslagMangledeOpplysningerBrevbestilling(request: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER
            mottaker = Mottakerroller.BRUKER
            fritekst = request.fritekst
            bestillersId = request.bestillersId
        }


    private fun lagInnvilgelseFolketrygdloven(request: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = request.kopiMottakere
            innledningFritekst = request.innledningFritekst
            begrunnelseFritekst = request.begrunnelseFritekst
            setTrygdeavtaleFritekst(request.trygdeavgiftFritekst)
            nyVurderingBakgrunn = request.nyVurderingBakgrunn
            ektefelleFritekst = request.ektefelleFritekst
            barnFritekst = request.barnFritekst
            bestillersId = request.bestillersId
        }


    private fun oppdaterBehandlingsresultat(behandlingID: Long, request: FattVedtakRequest) {
        if (request.behandlingsresultatTypeKode == Behandlingsresultattyper.OPPHØRT) {
            oppdaterBehandlingsresultatForOpphørt(behandlingID, request)
        } else {
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

            behandlingsresultat.type = request.behandlingsresultatTypeKode
            behandlingsresultat.settVedtakMetadata(request.vedtakstype, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
            behandlingsresultat.nyVurderingBakgrunn = request.nyVurderingBakgrunn
            behandlingsresultat.begrunnelseFritekst = request.begrunnelseFritekst
            behandlingsresultat.innledningFritekst = request.innledningFritekst
            behandlingsresultat.trygdeavgiftFritekst = request.trygdeavgiftFritekst
            behandlingsresultat.fastsattAvLand = Land_iso2.NO

            behandlingsresultatService.lagre(behandlingsresultat)
        }
    }

    private fun oppdaterBehandlingsresultatForOpphørt(behandlingID: Long, request: FattVedtakRequest) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val fullstendigManglendeInnbetaling = behandlingsresultat.avklartefakta
            .firstOrNull { Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode == it.referanse && Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING == it.type }
            ?: throw FunksjonellException("Forventer at fullstendigManglendeInnbetaling er satt ved fatting av vedtak for behandlingstype OPPHØRT")

        log.info("Fjerner vilkårsresultater, fritekstfelt, trygdeavgift, og virksomheter fra behandlingsresultat med behandlingsid: $behandlingID")
        behandlingsresultat.utfallRegistreringUnntak = null
        behandlingsresultat.nyVurderingBakgrunn = null
        behandlingsresultat.innledningFritekst = null
        behandlingsresultat.trygdeavgiftFritekst = null
        vilkaarsresultatService.tømVilkårForBehandlingsresultat(behandlingsresultat)

        behandlingsresultat.avklartefakta = setOf(fullstendigManglendeInnbetaling)
        behandlingsresultat.medlemAvFolketrygden.medlemskapsperioder = behandlingsresultat.medlemAvFolketrygden.medlemskapsperioder
            .filter { it.erInnvilget() }
            .onEach { it.innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT }
        behandlingsresultat.medlemAvFolketrygden.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD

        behandlingsresultat.type = Behandlingsresultattyper.OPPHØRT
        behandlingsresultat.settVedtakMetadata(request.vedtakstype, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
        behandlingsresultat.begrunnelseFritekst = request.begrunnelseFritekst
        behandlingsresultat.fastsattAvLand = Land_iso2.NO
        behandlingsresultatService.lagre(behandlingsresultat)
    }
}
