package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Behandling
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
    private val trygdeavtaleMapper: TrygdeavtaleMapper
) {
    fun mapBehandling(
        mottattBrevbestilling: DokgenBrevbestilling,
        mottaker: no.nav.melosys.domain.brev.Mottaker
    ): DokgenDto {
        // Henter opplysninger på nytt for å sikre at korrekt adresse benyttes (med mindre myndighet)
        val brevbestillingBuilder = mottattBrevbestilling.toBuilder()
        berikBestillingMedPersondata(brevbestillingBuilder, mottattBrevbestilling.behandling, mottaker)
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

    internal fun lagIkkeYrkesaktivVedtaksbrev(brevbestilling: IkkeYrkesaktivBrevbestilling): IkkeYrkesaktivVedtaksbrev {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandling.id)
        val lovvalgsperiode = behandlingsresultat.hentValidertPeriodeOmLovvalg()
        val mottatteOpplysningerData =
            behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadIkkeYrkesaktiv
        val oppholdsland = Land_iso2.valueOf(mottatteOpplysningerData.soeknadsland.landkoder.get(0)).beskrivelse
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
        return when (brevbestilling.produserbartdokument) {
            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD -> SaksbehandlingstidSoknad.av(
                brevbestilling.toBuilder()
                    .medAvsenderLand(dokgenMapperDatahenter.hentLandnavnFraLandkode(brevbestilling.avsenderLand))
                    .build(),
                Saksbehandlingstid.beregnSaksbehandlingsfrist(brevbestilling.forsendelseMottatt)
            )

            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> SaksbehandlingstidKlage.av(
                brevbestilling,
                Saksbehandlingstid.beregnSaksbehandlingsfrist(brevbestilling.forsendelseMottatt)
            )

            Produserbaredokumenter.MANGELBREV_BRUKER -> MangelbrevBruker.av(
                (brevbestilling as MangelbrevBrevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().id))
                    .build(),
                DokumentasjonSvarfrist.beregnFristPaaMangelbrevFraDagensDato()
            )

            Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER -> MangelbrevArbeidsgiver.av(
                (brevbestilling as MangelbrevBrevbestilling).toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.getBehandling().id))
                    .medFullmektigNavn(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD))
                    .build(),
                DokumentasjonSvarfrist.beregnFristPaaMangelbrevFraDagensDato()
            )

            Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN -> innvilgelseFtrlMapper.mapYrkesaktivFrivillig(brevbestilling as InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling)

            Produserbaredokumenter.PLIKTIG_MEDLEM_FTRL -> innvilgelseFtrlMapper.mapYrkesaktivPliktig(brevbestilling)

            Produserbaredokumenter.IKKE_YRKESAKTIV_FRIVILLIG_FTRL -> innvilgelseFtrlMapper.mapIkkeYrkesaktivFrivillig(brevbestilling)

            Produserbaredokumenter.IKKE_YRKESAKTIV_PLIKTIG_FTRL -> innvilgelseFtrlMapper.mapIkkeYrkesaktivPliktig(brevbestilling)

            Produserbaredokumenter.TRYGDEAVTALE_GB -> trygdeavtaleMapper.map(
                brevbestilling.toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandling.id))
                    .build() as InnvilgelseBrevbestilling, Land_iso2.GB
            )

            Produserbaredokumenter.TRYGDEAVTALE_US -> trygdeavtaleMapper.map(
                brevbestilling.toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandling.id))
                    .build() as InnvilgelseBrevbestilling, Land_iso2.US
            )

            Produserbaredokumenter.TRYGDEAVTALE_CAN -> trygdeavtaleMapper.map(
                brevbestilling.toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandling.id))
                    .build() as InnvilgelseBrevbestilling, Land_iso2.CA
            )

            Produserbaredokumenter.TRYGDEAVTALE_AU -> trygdeavtaleMapper.map(
                brevbestilling.toBuilder()
                    .medVedtaksdato(dokgenMapperDatahenter.hentVedtaksdato(brevbestilling.behandling.id))
                    .build() as InnvilgelseBrevbestilling, Land_iso2.AU
            )

            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER -> FritekstbrevBruker.av(
                (brevbestilling as FritekstbrevBrevbestilling).toBuilder()
                    .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD)).build(),
                Mottakerroller.BRUKER
            )

            Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET -> FritekstbrevVirksomhet.av(
                (brevbestilling as FritekstbrevBrevbestilling).toBuilder().build(), Mottakerroller.VIRKSOMHET
            )

            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER -> FritekstbrevBruker.av(
                (brevbestilling as FritekstbrevBrevbestilling).toBuilder()
                    .medNavnFullmektig(dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)).build(),
                Mottakerroller.ARBEIDSGIVER
            )

            Produserbaredokumenter.FRITEKSTBREV -> FritekstbrevNorskMyndighet.av(
                (brevbestilling as FritekstbrevBrevbestilling).toBuilder().build()
            )

            Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER -> Avslagbrev.av(
                (brevbestilling as AvslagBrevbestilling).toBuilder().build()
            )

            Produserbaredokumenter.MELDING_HENLAGT_SAK -> Henleggelsesbrev.av(
                (brevbestilling as HenleggelseBrevbestilling).toBuilder().build()
            )

            Produserbaredokumenter.GENERELT_FRITEKSTVEDLEGG -> Fritekstvedlegg.av(
                (brevbestilling as FritekstvedleggBrevbestilling).toBuilder().build()
            )

            Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV -> FritekstbrevTrygdemyndighet.av(
                brevbestilling as FritekstbrevBrevbestilling,
                Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
            )

            Produserbaredokumenter.IKKE_YRKESAKTIV_VEDTAKSBREV -> lagIkkeYrkesaktivVedtaksbrev(brevbestilling as IkkeYrkesaktivBrevbestilling)

            Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING -> VarselbrevManglendeInnbetaling(
                brevbestilling as VarselbrevManglendeInnbetalingBrevbestilling,
                brevbestilling.behandling.opprinneligBehandling?.id?.let {
                    val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(it)
                    behandlingsresultat.medlemAvFolketrygden?.medlemskapsperioder?.firstOrNull()?.medlemskapstype
                } ?: throw FunksjonellException("Forventer at behandling som tilhører varselbrevet har en opprinnelig behandling med medlemskapsperioder"),
                dokgenMapperDatahenter.hentFullmektigNavn(brevbestilling, Fullmaktstype.FULLMEKTIG_SØKNAD)
            )

            Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP -> VedtakOpphoertMedlemskap(brevbestilling as VedtakOpphoertMedlemskapBrevbestilling)

            else -> throw FunksjonellException("ProduserbartDokument ${brevbestilling.produserbartdokument} er ikke støttet av melosys-dokgen")
        }
    }
}
