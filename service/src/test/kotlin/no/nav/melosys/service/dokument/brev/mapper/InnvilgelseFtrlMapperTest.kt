package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
internal class InnvilgelseFtrlMapperTest {
    @MockK
    private lateinit var mockTrygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService

    @MockK
    private lateinit var mockAvklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    private lateinit var innvilgelseFtrlMapper: InnvilgelseFtrlMapper

    @BeforeEach
    fun setup() {
        innvilgelseFtrlMapper = InnvilgelseFtrlMapper(
            mockTrygdeavgiftsgrunnlagService,
            mockAvklarteVirksomheterService,
            mockDokgenMapperDatahenter
        )
    }

    @Test
    fun map_InnvilgetMedOmfattetFamilieKunNorskInntektFullstendigInnvilget_populererFelter() {
        mockHappyCase()

        innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling()).shouldNotBeNull()
            .apply {
                datoMottatt.shouldBe(LocalDate.EPOCH)
                perioder.shouldHaveSize(1)
                isErFullstendigInnvilget.shouldBeTrue()
                ftrl_2_8_begrunnelse.shouldBe(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT.kode)
                innvilgelse.apply {
                    innledningFritekst().shouldBeNull()
                    begrunnelseFritekst().shouldBe(BEGRUNNELSE_FRITEKST)
                    ektefelleFritekst().shouldBeNull()
                    barnFritekst().shouldBeNull()
                }
                saksbehandlerNavn.shouldBe(SAKSBEHANDLER_NAVN)
                arbeidsgiverNavn.shouldBe(ARBEIDSGIVER_NAVN)
                arbeidsland.shouldBe(Landkoder.AT.beskrivelse)
                isTrygdeavtaleMedArbeidsland.shouldBeTrue()
                vurderingTrygdeavgift.shouldNotBeNull().apply {
                    selvbetalende.shouldBeTrue()
                    representantNavn.shouldBeNull()
                    utenlandsk.shouldBeNull()
                    norsk.shouldNotBeNull().apply {
                        avgiftspliktigInntektMd() shouldBe 50000
                        trygdeavgiftNav().shouldBeFalse()
                        erSkattepliktig().shouldBeTrue()
                        arbeidsgiverBetalerAvgift().shouldBeTrue()
                        saerligeavgiftsgruppe().shouldBeNull()
                    }
                }
                loennsforhold.shouldBe(Loenn_forhold.LØNN_FRA_NORGE.kode)
                arbeidsgiverFullmektigNavn.shouldBeNull()
                isBrukerHarFullmektig.shouldBeFalse()
                avgiftssatsAar.shouldBe(DateTime.now().year.toString())
                isLoennNorgeSkattepliktig.shouldBeTrue()
                isLoennUtlandSkattepliktig.shouldBeFalse()
                saksinfo.apply {
                    fnr().shouldBe(DokgenTestData.FNR_BRUKER)
                    saksnummer().shouldBe(SAKSNUMMER)
                    navnBruker().shouldBe(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                }
                dagensDato.truncatedTo(ChronoUnit.DAYS).shouldBe(Instant.now().truncatedTo(ChronoUnit.DAYS))
                mottaker.apply {
                    adresselinjer().shouldNotBeEmpty()
                    postnr().shouldBe(DokgenTestData.POSTNR_BRUKER)
                    poststed().shouldBe(DokgenTestData.POSTSTED_BRUKER)
                    land().shouldBeNull()
                }
            }
    }

    @Test
    fun map_InnvilgetMedUtenlandskInntekt_harTrygdeavtaleMedLand_populererFelter() {
        mockHappyCase()

        every { mockTrygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(ofType()) } returns lagUtenlandskTrygdeAvgiftsgrunnlag()

        val behandlingsresultat = lagBehandlingsResultat()
        behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland =
            Soeknadsland(listOf("GB"), false)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat

        innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling()).apply {
            isTrygdeavtaleMedArbeidsland.shouldBeTrue()
            vurderingTrygdeavgift.shouldNotBeNull().apply {
                norsk.shouldBeNull()
                utenlandsk.shouldNotBeNull().apply {
                    avgiftspliktigInntektMd.shouldBe(50000)
                    trygdeavgiftNav.shouldBeTrue()
                    erSkattepliktig.shouldBeTrue()
                    arbeidsgiverBetalerAvgift.shouldBeFalse()
                    saerligeavgiftsgruppe.shouldBe(Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA.kode)
                }
            }
        }
    }

    @Test
    fun map_delvisInnvilget_populererFelter() {
        mockHappyCase()
        val delvisInnvilgetPeriode = Medlemskapsperiode()
        delvisInnvilgetPeriode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        delvisInnvilgetPeriode.innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
        delvisInnvilgetPeriode.medlemskapstype = Medlemskapstyper.FRIVILLIG
        delvisInnvilgetPeriode.setTrygdedekning(Trygdedekninger.HELSEDEL)
        val behandlingsresultat = lagBehandlingsResultat()
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        medlemAvFolketrygden.medlemskapsperioder = listOf(
            medlemAvFolketrygden.medlemskapsperioder.iterator().next(),
            delvisInnvilgetPeriode
        )
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat

        innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling()).apply {
            perioder.shouldHaveSize(2)
            isErFullstendigInnvilget.shouldBeFalse()
        }
    }

    private fun lagInnvilgelseBrevbestilling(): InnvilgelseBrevbestilling {
        return InnvilgelseBrevbestilling.Builder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .medForsendelseMottatt(Instant.EPOCH)
            .medBegrunnelseFritekst(BEGRUNNELSE_FRITEKST)
            .medSaksbehandlerNavn(SAKSBEHANDLER_NAVN)
            .build()
    }

    private fun lagNorskTrygdeAvgiftsgrunnlag(): Trygdeavgiftsgrunnlag =
        Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_NORGE, lagAvgiftsGrunnlagNorge(), null)

    private fun lagUtenlandskTrygdeAvgiftsgrunnlag(): Trygdeavgiftsgrunnlag =
        Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_UTLANDET, null, lagAvgiftsGrunnlagUtland())

    private fun lagAvgiftsGrunnlagNorge(): AvgiftsgrunnlagInfoNorge = AvgiftsgrunnlagInfoNorge(
        true, true, null,
        Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV
    )

    private fun lagAvgiftsGrunnlagUtland(): AvgiftsgrunnlagInfoUtland = AvgiftsgrunnlagInfoUtland(
        true, false, Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA,
        Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV
    )

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            medlemAvFolketrygden = lagMedlemAvFolketrygden()
            vilkaarsresultater = setOf(Vilkaarsresultat().apply {
                vilkaar = Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
                begrunnelser = setOf(VilkaarBegrunnelse().apply {
                    kode = Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT.kode
                })
            })
            behandling = DokgenTestData.lagBehandling()
        }
    }

    private fun lagAvklarteVirksomheter(): List<AvklartVirksomhet> = listOf(
        AvklartVirksomhet(
            ARBEIDSGIVER_NAVN,
            "987654321",
            BrevDataTestUtils.lagStrukturertAdresse(),
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )
    )

    private fun lagMedlemAvFolketrygden(): MedlemAvFolketrygden = MedlemAvFolketrygden().apply {
        medlemskapsperioder = lagMedlemskapsperioder()
        fastsattTrygdeavgift = lagFastsattTrygdeavgift()
    }

    private fun lagMedlemskapsperioder(): List<Medlemskapsperiode> = listOf(Medlemskapsperiode().apply {
        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER)
    })

    private fun lagFastsattTrygdeavgift(): FastsattTrygdeavgift = FastsattTrygdeavgift().apply {
        avgiftspliktigNorskInntektMnd = 50000L
        avgiftspliktigUtenlandskInntektMnd = 50000L
        betalesAv = Aktoer().apply {
            rolle = Aktoersroller.REPRESENTANT_TRYGDEAVGIFT
        }
        representantNr = "1234"
    }

    private fun mockHappyCase() {
        every { mockTrygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(ofType()) } returns lagNorskTrygdeAvgiftsgrunnlag()
        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(ofType()) } returns lagAvklarteVirksomheter()
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(ofType()) } returns "Østerrike"
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), any()) } returns null

//        whenever(mockTrygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(ArgumentMatchers.anyLong()))
//            .thenReturn(lagNorskTrygdeAvgiftsgrunnlag())
//        whenever(
//            mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(
//                ArgumentMatchers.any(
//                    Behandling::class.java
//                )
//            )
//        ).thenReturn(lagAvklarteVirksomheter())
//        whenever(mockDokgenMapperDatahenter.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
//            .thenReturn(lagBehandlingsResultat())
//        whenever(mockDokgenMapperDatahenter.hentLandnavnFraLandkode(ArgumentMatchers.anyString())).thenAnswer(
//            Answer { invocationOnMock: InvocationOnMock ->
//                Trygdeavtale_myndighetsland.valueOf(
//                    invocationOnMock.getArgument(
//                        0
//                    )
//                ).beskrivelse
//            } as Answer<String>)

    }

    companion object {
        const val BEGRUNNELSE_FRITEKST = "Begrunnelse fritekst"
        const val SAKSBEHANDLER_NAVN = "Fetter Anton"
        const val ARBEIDSGIVER_NAVN = "Bang Hansen"
        const val SAKSNUMMER = "MEL-123"
    }
}
