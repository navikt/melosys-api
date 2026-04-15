package no.nav.melosys.service.vedtak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
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
) : FattVedtakInterface {
    private val log = LoggerFactory.getLogger(FtrlVedtakService::class.java)

    override fun fattVedtak(behandling: Behandling, fattVedtakRequest: FattVedtakRequest) {
        val behandlingID = behandling.id
        log.info("Fatter vedtak for (FTRL) sak: ${behandling.fagsak.saksnummer} behandling: $behandlingID")

        val behandlingsresultat = oppdaterBehandlingsresultat(behandling, fattVedtakRequest)

        validerRequest(behandlingsresultat, fattVedtakRequest)

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw FunksjonellException("Det finnes allerede en vedtak-prosess for behandling $behandlingID")
        }

        val nyStatus =
            if (behandlingsresultat.type == Behandlingsresultattyper.OPPHØRT) Saksstatuser.OPPHØRT else Saksstatuser.LOVVALG_AVKLART
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK)
        prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(behandling, fattVedtakRequest.tilVedtakRequest(), nyStatus)
        dokgenService.produserOgDistribuerBrev(behandlingID, lagBrevbestilling(fattVedtakRequest, behandling, behandlingsresultat))
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id)
    }

    private fun validerRequest(behandlingsresultat: Behandlingsresultat, fattVedtakRequest: FattVedtakRequest) {
        if (fattVedtakRequest.behandlingsresultatTypeKode in listOf(Behandlingsresultattyper.OPPHØRT, Behandlingsresultattyper.DELVIS_OPPHØRT)) {
            val forventetOpphørsdato = behandlingsresultat.utledOpphørtDato()
            if (forventetOpphørsdato != fattVedtakRequest.opphørtDato) {
                throw FunksjonellException("Medsendt opphørsdato: ${fattVedtakRequest.opphørtDato} er ikke lik forventet opphørsdato: $forventetOpphørsdato")
            }
        }
    }

    private fun lagBrevbestilling(fattVedtakRequest: FattVedtakRequest, behandling: Behandling, behandlingsresultat: Behandlingsresultat): BrevbestillingDto {
        if (fattVedtakRequest.behandlingsresultatTypeKode == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return lagAvslagMangledeOpplysningerBrevbestilling(fattVedtakRequest)
        }
        if (fattVedtakRequest.behandlingsresultatTypeKode in listOf(Behandlingsresultattyper.OPPHØRT, Behandlingsresultattyper.DELVIS_OPPHØRT)) {
            return lagVedtakOpphørtMedlemskapBrevbestilling(fattVedtakRequest)
        }

        val behandlingstema = behandling.tema
        val medlemskapstype = behandlingsresultat.medlemskapsperioder.firstOrNull()?.hentMedlemskapstype()

        return when {
            behandlingstema.erIkkeYrkesaktiv() && medlemskapstype.erPliktig() ->
                lagBrevbestillingUtenFritekster(fattVedtakRequest, Produserbaredokumenter.IKKE_YRKESAKTIV_PLIKTIG_FTRL)

            behandlingstema.erIkkeYrkesaktiv() && medlemskapstype.erFrivillig() ->
                lagBrevbestillingUtenFritekster(fattVedtakRequest, Produserbaredokumenter.IKKE_YRKESAKTIV_FRIVILLIG_FTRL)

            behandlingstema.erYrkesaktiv() && medlemskapstype.erPliktig() ->
                lagBrevbestillingUtenFritekster(fattVedtakRequest, Produserbaredokumenter.PLIKTIG_MEDLEM_FTRL)

            behandlingstema.erYrkesaktiv() && medlemskapstype.erFrivillig() ->
                lagInnvilgelseFolketrygdloven(fattVedtakRequest)

            behandlingstema.erPensjonist() && medlemskapstype.erPliktig() ->
                lagInnvilgelsePensjonist(fattVedtakRequest, Produserbaredokumenter.PENSJONIST_PLIKTIG_FTRL)

            behandlingstema.erPensjonist() && medlemskapstype.erFrivillig() ->
                lagInnvilgelsePensjonist(fattVedtakRequest, Produserbaredokumenter.PENSJONIST_FRIVILLIG_FTRL)

            else -> throw FunksjonellException("Klarer ikke finne brev for kombinasjonen behandlingstema $behandlingstema og medlemskapstype $medlemskapstype")
        }
    }

    private fun lagVedtakOpphørtMedlemskapBrevbestilling(fattVedtakRequest: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = fattVedtakRequest.kopiMottakere
            begrunnelseFritekst = fattVedtakRequest.begrunnelseFritekst
            bestillersId = fattVedtakRequest.bestillersId
            opphørtDato = fattVedtakRequest.opphørtDato
        }

    private fun lagAvslagMangledeOpplysningerBrevbestilling(fattVedtakRequest: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER
            mottaker = Mottakerroller.BRUKER
            fritekst = fattVedtakRequest.fritekst
            bestillersId = fattVedtakRequest.bestillersId
        }

    private fun lagInnvilgelseFolketrygdloven(fattVedtakRequest: FattVedtakRequest): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = fattVedtakRequest.kopiMottakere
            innledningFritekst = fattVedtakRequest.innledningFritekst
            begrunnelseFritekst = fattVedtakRequest.begrunnelseFritekst
            setTrygdeavtaleFritekst(fattVedtakRequest.trygdeavgiftFritekst)
            nyVurderingBakgrunn = fattVedtakRequest.nyVurderingBakgrunn
            ektefelleFritekst = fattVedtakRequest.ektefelleFritekst
            barnFritekst = fattVedtakRequest.barnFritekst
            bestillersId = fattVedtakRequest.bestillersId
        }

    private fun lagBrevbestillingUtenFritekster(fattVedtakRequest: FattVedtakRequest, produserbaredokument: Produserbaredokumenter): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = produserbaredokument
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = fattVedtakRequest.kopiMottakere
            bestillersId = fattVedtakRequest.bestillersId
        }

    private fun lagInnvilgelsePensjonist(fattVedtakRequest: FattVedtakRequest, produserbaredokument: Produserbaredokumenter): BrevbestillingDto =
        BrevbestillingDto().apply {
            produserbardokument = produserbaredokument
            mottaker = Mottakerroller.BRUKER
            kopiMottakere = fattVedtakRequest.kopiMottakere
            innledningFritekst = fattVedtakRequest.innledningFritekst
            begrunnelseFritekst = fattVedtakRequest.begrunnelseFritekst
            setTrygdeavtaleFritekst(fattVedtakRequest.trygdeavgiftFritekst)
            nyVurderingBakgrunn = fattVedtakRequest.nyVurderingBakgrunn
            ektefelleFritekst = fattVedtakRequest.ektefelleFritekst
            bestillersId = fattVedtakRequest.bestillersId
        }

    private fun oppdaterBehandlingsresultat(behandling: Behandling, fattVedtakRequest: FattVedtakRequest): Behandlingsresultat {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        if (behandlingsresultat.harFullstendigManglendeInnbetaling()) {
            return oppdaterBehandlingsresultatForOpphørt(behandling.id, fattVedtakRequest)
        }

        return behandlingsresultat.apply {
            type = utledBehandlingsresultatType(this, fattVedtakRequest)
            settVedtakMetadata(fattVedtakRequest.vedtakstype, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
            nyVurderingBakgrunn = fattVedtakRequest.nyVurderingBakgrunn
            begrunnelseFritekst = fattVedtakRequest.begrunnelseFritekst
            innledningFritekst = fattVedtakRequest.innledningFritekst
            trygdeavgiftFritekst = fattVedtakRequest.trygdeavgiftFritekst
            fastsattAvLand = Land_iso2.NO
        }.let { behandlingsresultatService.lagreOgFlush(it) }
    }

    private fun Behandlingsresultat.harFullstendigManglendeInnbetaling(): Boolean =
        avklartefakta.any { it.type == Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING }

    private fun utledBehandlingsresultatType(
        behandlingsresultat: Behandlingsresultat,
        fattVedtakRequest: FattVedtakRequest
    ): Behandlingsresultattyper {
        // AVSLAG_MANGLENDE_OPPL er eksplisitt valg av saksbehandler
        if (fattVedtakRequest.behandlingsresultatTypeKode == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
        }

        val opphørtePerioder = behandlingsresultat.medlemskapsperioder.filter { it.erOpphørt() }

        if (opphørtePerioder.isNotEmpty()) {
            // Hvis alle perioder er opphørt, burde dette vært fanget av harFullstendigManglendeInnbetaling()
            if (opphørtePerioder.size == behandlingsresultat.medlemskapsperioder.size) {
                throw FunksjonellException(
                    "Alle medlemskapsperioder er opphørt, men FULLSTENDIG_MANGLENDE_INNBETALING mangler. " +
                    "Dette er en inkonsistent tilstand."
                )
            }
            return Behandlingsresultattyper.DELVIS_OPPHØRT
        }

        return Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    }

    private fun oppdaterBehandlingsresultatForOpphørt(behandlingID: Long, fattVedtakRequest: FattVedtakRequest): Behandlingsresultat {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val fullstendigManglendeInnbetaling = behandlingsresultat.avklartefakta
            .firstOrNull { it.type == Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING }
            ?: throw FunksjonellException("Forventer at fullstendigManglendeInnbetaling er satt ved fatting av vedtak for behandlingstype OPPHØRT")

        val opphørteMedlemskapsperioder = behandlingsresultat.medlemskapsperioder
            .filter { it.erInnvilget() || it.erOpphørt() }
            .onEach {
                it.innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT
                it.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
            }

        log.info("Fjerner vilkårsresultater, fritekstfelt, trygdeavgift, og virksomheter fra behandlingsresultat med behandlingsid: $behandlingID")
        behandlingsresultat.utfallRegistreringUnntak = null
        behandlingsresultat.nyVurderingBakgrunn = null
        behandlingsresultat.innledningFritekst = null
        behandlingsresultat.trygdeavgiftFritekst = null
        vilkaarsresultatService.tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat)

        behandlingsresultat.avklartefakta.clear()
        behandlingsresultat.avklartefakta.add(fullstendigManglendeInnbetaling)
        behandlingsresultat.medlemskapsperioder.clear()
        behandlingsresultat.medlemskapsperioder.addAll(opphørteMedlemskapsperioder)

        behandlingsresultat.type = Behandlingsresultattyper.OPPHØRT
        behandlingsresultat.settVedtakMetadata(fattVedtakRequest.vedtakstype, LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong()))
        behandlingsresultat.begrunnelseFritekst = fattVedtakRequest.begrunnelseFritekst
        behandlingsresultat.fastsattAvLand = Land_iso2.NO
        return behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun Behandlingstema.erPensjonist(): Boolean = this == Behandlingstema.PENSJONIST
    private fun Behandlingstema.erYrkesaktiv(): Boolean = this == Behandlingstema.YRKESAKTIV
    private fun Behandlingstema.erIkkeYrkesaktiv(): Boolean = this == Behandlingstema.IKKE_YRKESAKTIV
    private fun Medlemskapstyper?.erPliktig(): Boolean = this == Medlemskapstyper.PLIKTIG
    private fun Medlemskapstyper?.erFrivillig(): Boolean = this == Medlemskapstyper.FRIVILLIG
}
