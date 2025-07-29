package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.*
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.mottatteopplysninger.SøknadIkkeYrkesaktiv
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.*
import no.nav.melosys.integrasjon.dokgen.dto.felles.Mottaker
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

@Component
class DokgenMalMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val innvilgelseFtrlMapper: InnvilgelseFtrlMapper,
    private val innvilgelseEftaMapper: InnvilgelseEftaStorbritanniaMapper,
    private val innhentingAvInntektsopplysningerMapper: InnhentingAvInntektsopplysningerMapper,
    private val trygdeavtaleMapper: TrygdeavtaleMapper,
    private val orienteringAnmodningUnntakMapper: OrienteringAnmodningUnntakMapper,
    private val orienteringTilArbeidsgiverOmVedtakMapper: OrienteringTilArbeidsgiverOmVedtakMapper,
    private val årsavregningVedtakMapper: ÅrsavregningVedtakMapper,
    private val informasjonTrygdeavgiftMapper: InformasjonTrygdeavgiftMapper
) {
    fun mapBehandling(
        mottattBrevbestilling: DokgenBrevbestilling,
        mottaker: no.nav.melosys.domain.brev.Mottaker
    ): DokgenDto {
        // Henter opplysninger på nytt for å sikre at korrekt adresse benyttes (med mindre myndighet)
        val brevbestillingBuilder = mottattBrevbestilling.toBuilder()
        berikBestillingMedPersondata(brevbestillingBuilder, mottattBrevbestilling.behandlingNonNull(), mottaker)
        return lagDokgenDtoFraBestilling(brevbestillingBuilder.build()).apply {
            if (Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET.kode != this.mottaker.type) {
                this.mottaker = lagMottakerUtenKoder(this.mottaker)
            }
        }
    }

    private fun lagMottakerUtenKoder(mottakerMedKoder: Mottaker): Mottaker {
        var poststed = mottakerMedKoder.poststed
        if (Landkoder.NO.kode == mottakerMedKoder.land && StringUtils.hasText(mottakerMedKoder.postnr)) {
            poststed = dokgenMapperDatahenter.hentNorskPoststed(mottakerMedKoder.postnr)
        }
        val land = dokgenMapperDatahenter.hentLandnavnFraLandkode(mottakerMedKoder.land)
        return Mottaker(
            mottakerMedKoder.navn,
            mottakerMedKoder.adresselinjer,
            mottakerMedKoder.postnr,
            poststed,
            land,
            mottakerMedKoder.type,
            mottakerMedKoder.region
        )
    }

    private fun berikBestillingMedPersondata(
        mottattBrevbestilling: DokgenBrevbestilling.Builder<*>,
        behandling: Behandling,
        mottaker: no.nav.melosys.domain.brev.Mottaker
    ) {
        mottattBrevbestilling
            .medPersonDokument(dokgenMapperDatahenter.hentPersondata(behandling))
            .medPersonMottaker(dokgenMapperDatahenter.hentPersonMottaker(mottaker))
    }

    internal fun lagVedtakOpphoertMedlemskap(brevbestilling: VedtakOpphoertMedlemskapBrevbestilling): VedtakOpphoertMedlemskap {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingNonNull().id)
        val land = hentLandForVedtakOpphoertMedlemskap(behandlingsresultat)

        return VedtakOpphoertMedlemskap.av(
            brevbestilling.toBuilder()
                .medLand(land)
                .medBehandlingstema(behandlingsresultat.behandling.tema.name)
                .build()
        )
    }

    private fun hentLandForVedtakOpphoertMedlemskap(behandlingsresultat: Behandlingsresultat): List<String> {
        val mottatteOpplysningerData = behandlingsresultat.behandling.mottatteOpplysninger?.mottatteOpplysningerData
        return mottatteOpplysningerData?.soeknadsland?.landkoder?.map(dokgenMapperDatahenter::hentLandnavnFraLandkode) ?: emptyList()
    }

    internal fun lagIkkeYrkesaktivVedtaksbrev(brevbestilling: IkkeYrkesaktivBrevbestilling): IkkeYrkesaktivVedtaksbrev {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingNonNull().id)
        val lovvalgsperiode = behandlingsresultat.hentValidertPeriodeOmLovvalg()
        val mottatteOpplysningerData =
            behandlingsresultat.behandling.mottatteOpplysninger!!.mottatteOpplysningerData as SøknadIkkeYrkesaktiv
        val oppholdsland = Land_iso2.valueOf(mottatteOpplysningerData.soeknadsland.landkoder[0]).beskrivelse
        val bestemmelse = lovvalgsperiode.bestemmelse
        val bestemmelseBeskrivelse = bestemmelse.beskrivelse
        val artikkel = bestemmelseBeskrivelse.substringAfterLast('-', bestemmelseBeskrivelse).trim()

        return IkkeYrkesaktivVedtaksbrev.av(
            brevbestilling.toBuilder()
                .medBegrunnelseFritekst(behandlingsresultat.begrunnelseFritekst)
                .medInnledningFritekst(behandlingsresultat.innledningFritekst)
                .medNyVurderingBakgrunn(behandlingsresultat.nyVurderingBakgrunn)
                .medOppholdsLand(oppholdsland)
                .medPeriodeFom(lovvalgsperiode.fom)
                .medPeriodeTom(lovvalgsperiode.tom)
                .medBestemmelse(bestemmelse.name())
                .medIkkeyrkesaktivSituasjontype(mottatteOpplysningerData.ikkeYrkesaktivSituasjontype)
                .medArtikkel(artikkel)
                .build()
        )
    }

    private fun lagDokgenDtoFraBestilling(brevbestilling: DokgenBrevbestilling): DokgenDto {
        return when (val produserbartDokument = brevbestilling.produserbartdokument) {
            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD ->
                lagSaksbehandlingstidSoknad(brevbestilling)

            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE ->
                lagSaksbehandlingstidKlage(brevbestilling)

            Produserbaredokumenter.MANGELBREV_BRUKER ->
                lagMangelbrevBruker(brevbestilling as MangelbrevBrevbestilling)

            Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER ->
                lagMangelbrevArbeidsgiver(brevbestilling as MangelbrevBrevbestilling)

            Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN ->
                innvilgelseFtrlMapper.mapYrkesaktivFrivillig(brevbestilling as InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling)

            Produserbaredokumenter.INNVILGELSE_EFTA_STORBRITANNIA ->
                innvilgelseEftaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling as InnvilgelseEftaStorbritanniaBrevbestilling)

            Produserbaredokumenter.ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK ->
                orienteringTilArbeidsgiverOmVedtakMapper.map(brevbestilling as OrienteringTilArbeidsgiverOmVedtakBrevbestilling)

            Produserbaredokumenter.PLIKTIG_MEDLEM_FTRL ->
                innvilgelseFtrlMapper.mapYrkesaktivPliktig(brevbestilling)

            Produserbaredokumenter.PENSJONIST_PLIKTIG_FTRL ->
                innvilgelseFtrlMapper.mapPensjonistPliktig(brevbestilling)

            Produserbaredokumenter.PENSJONIST_FRIVILLIG_FTRL ->
                innvilgelseFtrlMapper.mapPensjonistFrivillig(brevbestilling)

            Produserbaredokumenter.TRYGDEAVGIFT_INFORMASJONSBREV ->
                informasjonTrygdeavgiftMapper.mapInformasjonTrygdeavgift(brevbestilling)

            Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER ->
                innhentingAvInntektsopplysningerMapper.map(brevbestilling as InnhentingAvInntektsopplysningerBrevbestilling)

            Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK ->
                orienteringAnmodningUnntakMapper.map(brevbestilling as OrienteringAnmodningUnntakBrevbestilling)

            Produserbaredokumenter.IKKE_YRKESAKTIV_FRIVILLIG_FTRL ->
                innvilgelseFtrlMapper.mapIkkeYrkesaktivFrivillig(brevbestilling)

            Produserbaredokumenter.IKKE_YRKESAKTIV_PLIKTIG_FTRL ->
                innvilgelseFtrlMapper.mapIkkeYrkesaktivPliktig(brevbestilling)

            Produserbaredokumenter.TRYGDEAVTALE_GB,
            Produserbaredokumenter.TRYGDEAVTALE_US,
            Produserbaredokumenter.TRYGDEAVTALE_CAN,
            Produserbaredokumenter.TRYGDEAVTALE_AU ->
                lagTrygdeavtaleDokument(brevbestilling, produserbartDokument)

            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER ->
                lagFritekstbrevBruker(brevbestilling as FritekstbrevBrevbestilling)

            Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET ->
                lagFritekstbrevVirksomhet(brevbestilling as FritekstbrevBrevbestilling)

            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER ->
                lagFritekstbrevArbeidsgiver(brevbestilling as FritekstbrevBrevbestilling)

            Produserbaredokumenter.FRITEKSTBREV ->
                FritekstbrevNorskMyndighet.av((brevbestilling as FritekstbrevBrevbestilling).toBuilder().build())

            Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER ->
                Avslagbrev.av((brevbestilling as AvslagBrevbestilling).toBuilder().build())

            Produserbaredokumenter.MELDING_HENLAGT_SAK ->
                Henleggelsesbrev.av((brevbestilling as HenleggelseBrevbestilling).toBuilder().build())

            Produserbaredokumenter.GENERELT_FRITEKSTVEDLEGG ->
                Fritekstvedlegg.av((brevbestilling as FritekstvedleggBrevbestilling).toBuilder().build())

            Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV ->
                FritekstbrevTrygdemyndighet.av(brevbestilling as FritekstbrevBrevbestilling, Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)

            Produserbaredokumenter.IKKE_YRKESAKTIV_VEDTAKSBREV ->
                lagIkkeYrkesaktivVedtaksbrev(brevbestilling as IkkeYrkesaktivBrevbestilling)

            Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING ->
                lagVarselbrevManglendeInnbetaling(brevbestilling as VarselbrevManglendeInnbetalingBrevbestilling)

            Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP ->
                lagVedtakOpphoertMedlemskap(brevbestilling as VedtakOpphoertMedlemskapBrevbestilling)

            Produserbaredokumenter.AVSLAG_EFTA_STORBRITANNIA ->
                lagAvslagEftaStorbritannia(brevbestilling as AvslagEftaStorbritanniaBrevbestilling)

            Produserbaredokumenter.AARSAVREGNING_VEDTAKSBREV ->
                lagÅrsavregningVedtak(brevbestilling as ÅrsavregningVedtakBrevBestilling)

            else -> throw FunksjonellException("ProduserbartDokument $produserbartDokument er ikke støttet av melosys-dokgen")
        }
    }

    private fun lagSaksbehandlingstidSoknad(brevbestilling: DokgenBrevbestilling): SaksbehandlingstidSoknad {
        return SaksbehandlingstidSoknad.av(
            brevbestilling.toBuilder()
                .medAvsenderLand(dokgenMapperDatahenter.hentLandnavnFraLandkode(brevbestilling.avsenderLand))
                .build(),
            Saksbehandlingstid.beregnSaksbehandlingsfrist(brevbestilling.forsendelseMottatt)
        )
    }

    private fun lagSaksbehandlingstidKlage(brevbestilling: DokgenBrevbestilling): SaksbehandlingstidKlage {
        return SaksbehandlingstidKlage.av(
            brevbestilling,
            Saksbehandlingstid.beregnSaksbehandlingsfrist(brevbestilling.forsendelseMottatt)
        )
    }

    private fun lagMangelbrevBruker(brevbestilling: MangelbrevBrevbestilling): MangelbrevBruker {
        return MangelbrevBruker.av(
            brevbestilling.toBuilder()
                .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandlingNonNull().id))
                .build(),
            DokumentasjonSvarfrist.beregnFristPaaMangelbrevFraDagensDato()
        )
    }

    private fun lagMangelbrevArbeidsgiver(brevbestilling: MangelbrevBrevbestilling): MangelbrevArbeidsgiver {
        return MangelbrevArbeidsgiver.av(
            brevbestilling.toBuilder()
                .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandlingNonNull().id))
                .medFullmektigNavn(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD))
                .build(),
            DokumentasjonSvarfrist.beregnFristPaaMangelbrevFraDagensDato()
        )
    }

    private fun lagTrygdeavtaleDokument(brevbestilling: DokgenBrevbestilling, produserbartDokument: Produserbaredokumenter): DokgenDto {
        val land = when(produserbartDokument) {
            Produserbaredokumenter.TRYGDEAVTALE_GB -> Land_iso2.GB
            Produserbaredokumenter.TRYGDEAVTALE_US -> Land_iso2.US
            Produserbaredokumenter.TRYGDEAVTALE_CAN -> Land_iso2.CA
            Produserbaredokumenter.TRYGDEAVTALE_AU -> Land_iso2.AU
            else -> throw FunksjonellException("Ukjent trygdeavtale: $produserbartDokument")
        }

        return trygdeavtaleMapper.map(
            brevbestilling.toBuilder()
                .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandlingNonNull().id))
                .build() as InnvilgelseBrevbestilling,
            land
        )
    }

    private fun lagFritekstbrevBruker(brevbestilling: FritekstbrevBrevbestilling): FritekstbrevBruker {
        return FritekstbrevBruker.av(
            brevbestilling.toBuilder()
                .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD))
                .build(),
            Mottakerroller.BRUKER
        )
    }

    private fun lagFritekstbrevVirksomhet(brevbestilling: FritekstbrevBrevbestilling): FritekstbrevVirksomhet {
        return FritekstbrevVirksomhet.av(brevbestilling.toBuilder().build(), Mottakerroller.VIRKSOMHET)
    }

    private fun lagFritekstbrevArbeidsgiver(brevbestilling: FritekstbrevBrevbestilling): FritekstbrevBruker {
        return FritekstbrevBruker.av(
            brevbestilling.toBuilder()
                .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER))
                .build(),
            Mottakerroller.ARBEIDSGIVER
        )
    }

    private fun lagVarselbrevManglendeInnbetaling(brevbestilling: VarselbrevManglendeInnbetalingBrevbestilling): VarselbrevManglendeInnbetaling {
        val medlemskapstype = brevbestilling.behandlingNonNull().id.let {
            val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(it)
            behandlingsresultat.medlemskapsperioder.firstOrNull()?.medlemskapstype
        } ?: throw FunksjonellException("Forventer at behandling som tilhører varselbrevet har en opprinnelig behandling med medlemskapsperioder")

        return VarselbrevManglendeInnbetaling(
            brevbestilling,
            medlemskapstype,
            dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD)
        )
    }

    private fun lagAvslagEftaStorbritannia(brevbestilling: AvslagEftaStorbritanniaBrevbestilling): AvslagEftaStorbritannia {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val lovvalgsperiode = behandlingsresultat.hentValidertPeriodeOmLovvalg()
        val virksomhetNavn = dokgenMapperDatahenter.hentAvklartVirksomhet(brevbestilling.behandling).navn

        return AvslagEftaStorbritannia(brevbestilling, lovvalgsperiode, virksomhetNavn)
    }

    private fun lagÅrsavregningVedtak(brevbestilling: ÅrsavregningVedtakBrevBestilling): DokgenDto {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        return årsavregningVedtakMapper.mapÅrsavregning(brevbestilling, behandlingsresultat)
    }
}
