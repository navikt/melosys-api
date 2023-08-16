package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
internal class InnvilgelseFtrlMapperTest {

    @MockK
    private lateinit var mockAvklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    private lateinit var innvilgelseFtrlMapper: InnvilgelseFtrlMapper

    @BeforeEach
    fun setup() {
        innvilgelseFtrlMapper = InnvilgelseFtrlMapper(
            mockAvklarteVirksomheterService,
            mockDokgenMapperDatahenter
        )
    }

    @Test
    fun map_InnvilgetKunNorskInntektInnvilget_populererFelter() {
        mockHappyCase()

        innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling()).shouldNotBeNull()
            .apply {
                saksbehandlerNavn.shouldBe(SAKSBEHANDLER_NAVN)
                saksinfo.shouldBeInstanceOf<SaksinfoBruker>().apply {
                    fnr.shouldBe(DokgenTestData.FNR_BRUKER)
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

                datoMottatt.shouldBe(LocalDate.EPOCH)
                innvilgelse.apply {
                    innledningFritekst.shouldBeNull()
                    begrunnelseFritekst.shouldBe(BEGRUNNELSE_FRITEKST)
                    ektefelleFritekst.shouldBeNull()
                    barnFritekst.shouldBeNull()
                }
                brukerHarFullmektig.shouldBeFalse()
                perioder.shouldHaveSize(1).first().apply {
                    innvilgelsesResultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8)
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
                ftrl_2_8_begrunnelse.shouldBe(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANNEN_GRUNN)
                begrunnelseAnnenGrunnFritekst.shouldBe("<p>Vilkårresultat begrunnelse fritekst</p>")
                arbeidsgivere.shouldHaveSize(1).first().shouldBe(ARBEIDSGIVER_NAVN)
                arbeidsland.shouldBe(Landkoder.AT.beskrivelse)
                trygdeavtaleMedArbeidsland.shouldBeFalse()
                arbeidsgiverFullmektigNavn.shouldBeNull()
                betalerArbeidsgiveravgift.shouldBeTrue()
            }
    }

    @Test
    fun map_InnvilgetMedUtenlandskInntekt_harTrygdeavtaleMedLand_populererFelter() {
        mockHappyCase()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.GB.kode) } returns Landkoder.GB.beskrivelse
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat().apply {
            behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland =
                Soeknadsland(listOf(Landkoder.GB.kode), false)
        }

        innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling()).apply {
            arbeidsland.shouldBe(Landkoder.GB.beskrivelse)
            trygdeavtaleMedArbeidsland.shouldBeTrue()
        }
    }

    @Test
    fun map_delvisInnvilget_populererFelter() {
        mockHappyCase()
        val behandlingsresultat = lagBehandlingsResultat()
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat.apply {
            medlemAvFolketrygden.medlemskapsperioder = listOf(
                medlemAvFolketrygden.medlemskapsperioder.iterator().next(),
                Medlemskapsperiode().apply {
                    fom = LocalDate.EPOCH.plusMonths(1)
                    tom = LocalDate.EPOCH.plusMonths(4)
                    innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.HELSEDEL
                    medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
                }
            )
        }

        innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling()).apply {
            perioder.shouldHaveSize(2)
                .map { it.innvilgelsesResultat }
                .shouldContainExactlyInAnyOrder(InnvilgelsesResultat.INNVILGET, InnvilgelsesResultat.DELVIS_INNVILGET)
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


    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            medlemAvFolketrygden = lagMedlemAvFolketrygden()
            vilkaarsresultater = setOf(Vilkaarsresultat().apply {
                vilkaar = Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
                begrunnelser = setOf(lagVilkaarBegrunnelse(this))
            })
            behandling = DokgenTestData.lagBehandling()
        }
    }

    private fun lagVilkaarBegrunnelse(vilkårsresultat: Vilkaarsresultat): VilkaarBegrunnelse =
        VilkaarBegrunnelse().apply {
            kode = Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANNEN_GRUNN.kode
            vilkaarsresultat = vilkårsresultat.apply {
                begrunnelseFritekst = "<p>Vilkårresultat begrunnelse fritekst</p>"
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
        medlemskapsperioder = lagMedlemskapsperioder(this)
        fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                inntektsperioder = listOf(Inntektsperiode().apply { isOrdinærTrygdeavgiftBetalesTilSkatt = true })
                skatteforholdTilNorge =
                    setOf(SkatteforholdTilNorge().apply { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG })
            }
            trygdeavgiftsperioder = lagTrygdeavgiftsperioder(medlemskapsperioder.first())
        }
        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
    }

    private fun lagMedlemskapsperioder(medlemAvFolketrygden: MedlemAvFolketrygden): List<Medlemskapsperiode> =
        listOf(Medlemskapsperiode().apply {
            fom = LocalDate.EPOCH.plusMonths(1)
            tom = LocalDate.EPOCH.plusMonths(4)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER
            this.medlemAvFolketrygden = medlemAvFolketrygden
        })

    private fun lagTrygdeavgiftsperioder(medlemskapsperiode: Medlemskapsperiode): Set<Trygdeavgiftsperiode> =
        setOf(Trygdeavgiftsperiode().apply {
            periodeFra = LocalDate.EPOCH.plusMonths(1)
            periodeTil = LocalDate.EPOCH.plusMonths(4)
            trygdesats = BigDecimal.ZERO
            trygdeavgiftsbeløpMd = Penger(0.0)
            grunnlagInntekstperiode = lagGrunnlagInntektsperiode()
            grunnlagMedlemskapsperiode = medlemskapsperiode
        })

    private fun lagGrunnlagInntektsperiode(): Inntektsperiode =
        Inntektsperiode().apply {
            type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
            isArbeidsgiversavgiftBetalesTilSkatt = true
            avgiftspliktigInntektMnd = Penger(0.0)
        }

    private fun mockHappyCase() {
        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(ofType()) } returns lagAvklarteVirksomheter()
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat()
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.AT.kode) } returns Landkoder.AT.beskrivelse
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), any()) } returns null
    }

    companion object {
        const val BEGRUNNELSE_FRITEKST = "Begrunnelse fritekst"
        const val SAKSBEHANDLER_NAVN = "Fetter Anton"
        const val ARBEIDSGIVER_NAVN = "Bang Hansen"
        const val SAKSNUMMER = "MEL-123"
    }
}
