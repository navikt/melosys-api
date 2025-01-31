package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.*
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartUkjentSluttdatoMedlemskapsperiodeService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
internal class InnvilgelseFtrlYrkesaktivFrivilligMapperTest {

    @MockK
    private lateinit var mockAvklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var mockAvklartUkjentSluttdatoMedlemskapsperiodeService: AvklartUkjentSluttdatoMedlemskapsperiodeService

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var innvilgelseFtrlMapper: InnvilgelseFtrlMapper

    @BeforeEach
    fun setup() {
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(mockBehandlingsresultatService)
        innvilgelseFtrlMapper = InnvilgelseFtrlMapper(
            mockAvklarteVirksomheterService,
            mockAvklartUkjentSluttdatoMedlemskapsperiodeService,
            mockDokgenMapperDatahenter,
            trygdeavgiftMottakerService,
            trygdeavgiftsberegningService,
        )
    }

    @Test
    fun mapYrkesaktivFrivillig_InnvilgetKunNorskInntektInnvilget_populererFelter() {
        mockHappyCase(Case.paragraf_2_8)

        innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).shouldNotBeNull()
            .apply {
                behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
                nyVurderingBakgrunn.shouldBe("NYE_OPPLYSNINGER")
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
                }

                datoMottatt.shouldBe(LocalDate.EPOCH)
                innledningFritekst.shouldBeNull()
                begrunnelseFritekst.shouldBe(BEGRUNNELSE_FRITEKST)
                trygdeavgiftFritekst.shouldBe(TRYGDEAVGIFT_FRITEKST)
                avgiftsperioder.shouldHaveSize(2)
                medlemskapsperioder.shouldHaveSize(1).first().apply {
                    innvilgelsesResultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8)
                avslåttMedlemskapsperiodeFørMottaksdatoHelsedel.shouldBe(false)
                avslåttMedlemskapsperiodeFørMottaksdatoFullDekning.shouldBe(false)
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
                begrunnelse.shouldBe(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANNEN_GRUNN)
                begrunnelseAnnenGrunnFritekst.shouldBe("<p>Vilkårresultat begrunnelse fritekst</p>")
                arbeidsgivere.shouldHaveSize(1).first().shouldBe(ARBEIDSGIVER_NAVN)
                flereLandUkjentHvilke.shouldBeFalse()
                land.shouldContainOnly(Landkoder.AT.beskrivelse)
                trygdeavtaleLand.shouldBeEmpty()
                betalerArbeidsgiveravgift.shouldBeTrue()
                ukjentSluttdatoMedlemskapsperiode.shouldBeTrue()
            }
    }

    @Test
    fun mapYrkesaktivFrivillig_harTrygdeavtaleLand_populererFelter() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8).apply {
            behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland = Soeknadsland(listOf(Landkoder.GB.kode), false)
        }
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.GB.kode) } returns Landkoder.GB.beskrivelse
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockBehandlingsresultatService.hentBehandlingsresultat(ofType()) } returns behandlingsresultat

        innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).apply {
            flereLandUkjentHvilke.shouldBeFalse()
            land.shouldContainOnly(Landkoder.GB.beskrivelse)
            trygdeavtaleLand.shouldContainOnly(Landkoder.GB.beskrivelse)
        }
    }

    @Test
    fun mapYrkesaktivFrivillig_harFlereLandUkjentHvilke_populererFelter() {
        mockHappyCase(Case.paragraf_2_8)
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsResultat(Case.paragraf_2_8).apply {
            behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland = Soeknadsland(emptyList(), true)
        }

        innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).apply {
            flereLandUkjentHvilke.shouldBeTrue()
            land.shouldBeEmpty()
            trygdeavtaleLand.shouldBeEmpty()
        }
    }


    @Test
    fun mapYrkesaktivFrivillig_innvilgetOgAvslaatt_populererFelter() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat.apply {
            behandlingsresultat.medlemskapsperioder = listOf(
                behandlingsresultat.medlemskapsperioder.iterator().next(),
                Medlemskapsperiode().apply {
                    fom = LocalDate.EPOCH.minusMonths(1)
                    tom = LocalDate.EPOCH.minusMonths(4)
                    innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                    this.behandlingsresultat = behandlingsresultat
                }
            )
        }

        innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).apply {
            innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).shouldNotBeNull()
                .apply {
                    behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
                    nyVurderingBakgrunn.shouldBe("NYE_OPPLYSNINGER")
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
                    }

                    datoMottatt.shouldBe(LocalDate.EPOCH)
                    innledningFritekst.shouldBeNull()
                    begrunnelseFritekst.shouldBe(BEGRUNNELSE_FRITEKST)
                    trygdeavgiftFritekst.shouldBe(TRYGDEAVGIFT_FRITEKST)
                    avgiftsperioder.shouldHaveSize(2)
                    medlemskapsperioder.shouldHaveSize(2).first().apply {
                        innvilgelsesResultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                    }
                    medlemskapsperioder.shouldHaveSize(2).last().apply {
                        innvilgelsesResultat.shouldBe(InnvilgelsesResultat.AVSLAATT)
                    }
                    bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8)
                    avslåttMedlemskapsperiodeFørMottaksdatoHelsedel.shouldBe(true)
                    avslåttMedlemskapsperiodeFørMottaksdatoFullDekning.shouldBe(false)
                    skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
                    begrunnelse.shouldBe(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANNEN_GRUNN)
                    begrunnelseAnnenGrunnFritekst.shouldBe("<p>Vilkårresultat begrunnelse fritekst</p>")
                    arbeidsgivere.shouldHaveSize(1).first().shouldBe(ARBEIDSGIVER_NAVN)
                    flereLandUkjentHvilke.shouldBeFalse()
                    land.shouldContainOnly(Landkoder.AT.beskrivelse)
                    trygdeavtaleLand.shouldBeEmpty()
                    betalerArbeidsgiveravgift.shouldBeTrue()
                    ukjentSluttdatoMedlemskapsperiode.shouldBeTrue()
                }
        }
    }

    @Test
    fun mapYrkesaktivFrivillig_innvilgetOgAvslaatt_populererFelter_ingen_avgiftsperioder() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)
        val trygdeavgiftsperioder = mutableSetOf(
            Trygdeavgiftsperiode(
                grunnlagInntekstperiode = lagGrunnlagInntektsperiode(),
                grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG },
                trygdesats = BigDecimal.ZERO,
                trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
                periodeFra = LocalDate.EPOCH.plusMonths(1),
                periodeTil = LocalDate.EPOCH.plusMonths(4)
            )
        )
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat.apply {
            behandlingsresultat.medlemskapsperioder = listOf(
                Medlemskapsperiode().apply {
                    fom = LocalDate.EPOCH.plusMonths(1)
                    tom = LocalDate.EPOCH.plusMonths(4)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    medlemskapstype = Medlemskapstyper.FRIVILLIG
                    trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                    this.behandlingsresultat = behandlingsresultat
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
                    this.trygdeavgiftsperioder = trygdeavgiftsperioder
                }
            )
        }

        innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).apply {
            avgiftsperioder.shouldHaveSize(0)
            medlemskapsperioder.shouldHaveSize(1)
                .map { it.innvilgelsesResultat }
                .shouldContainExactlyInAnyOrder(InnvilgelsesResultat.INNVILGET)
        }
    }

    @Test
    fun `mapYrkesaktivFrivillig Innvilget 2_7 populerer felter`() {
        mockHappyCase(Case.paragraf_2_7)

        innvilgelseFtrlMapper.mapYrkesaktivFrivillig(lagBrevbestilling()).shouldNotBeNull()
            .apply {
                behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
                nyVurderingBakgrunn.shouldBe("NYE_OPPLYSNINGER")
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
                }

                datoMottatt.shouldBe(LocalDate.EPOCH)
                innledningFritekst.shouldBeNull()
                begrunnelseFritekst.shouldBe(BEGRUNNELSE_FRITEKST)
                trygdeavgiftFritekst.shouldBe(TRYGDEAVGIFT_FRITEKST)
                avgiftsperioder.shouldHaveSize(2)
                medlemskapsperioder.shouldHaveSize(1).first().apply {
                    innvilgelsesResultat.shouldBe(InnvilgelsesResultat.INNVILGET)
                }
                bestemmelse.shouldBe(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD)
                avslåttMedlemskapsperiodeFørMottaksdatoHelsedel.shouldBe(false)
                avslåttMedlemskapsperiodeFørMottaksdatoFullDekning.shouldBe(false)
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
                begrunnelse.shouldBe(Ftrl_2_7_begrunnelser.ANNEN_GRUNN)
                begrunnelseAnnenGrunnFritekst.shouldBe("<p>Vilkårresultat begrunnelse fritekst</p>")
                arbeidsgivere.shouldHaveSize(1).first().shouldBe(ARBEIDSGIVER_NAVN)
                flereLandUkjentHvilke.shouldBeFalse()
                land.shouldContainOnly(Landkoder.AT.beskrivelse)
                trygdeavtaleLand.shouldBeEmpty()
                betalerArbeidsgiveravgift.shouldBeTrue()
                ukjentSluttdatoMedlemskapsperiode.shouldBeTrue()
            }
    }

    @Test
    fun `mapYrkesaktivPliktig - bruker 68 år gammel har ikke lav sats`() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)

        val foedselsdato = LocalDate.of(1955, 1, 1)
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)  // Alder: 68
            tom = LocalDate.of(2023, 12, 31) // Alder: 68
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        }

        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata(foedselsdato)

        val resultat = innvilgelseFtrlMapper.mapYrkesaktivPliktig(lagBrevbestilling())

        resultat.harLavSatsPgaAlder.shouldBeFalse()
    }

    @Test
    fun `mapYrkesaktivPliktig - bruker 69 år gammel har ikke lav sats pga TILLEGGSAVTALE_NATO`() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)

        val foedselsdato = LocalDate.of(1954, 1, 1)
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)  // Alder: 69
            tom = LocalDate.of(2023, 12, 31) // Alder: 69
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = TILLEGGSAVTALE_NATO
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        }

        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata(foedselsdato)

        val resultat = innvilgelseFtrlMapper.mapYrkesaktivPliktig(lagBrevbestilling())

        resultat.harLavSatsPgaAlder.shouldBeFalse()
    }

    @Test
    fun `mapYrkesaktivPliktig - bruker 69 år gammel har lav sats`() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)

        val foedselsdato = LocalDate.of(1954, 1, 1)
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)  // Alder: 69
            tom = LocalDate.of(2023, 12, 31) // Alder: 69
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        }

        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata(foedselsdato)

        val resultat = innvilgelseFtrlMapper.mapYrkesaktivPliktig(lagBrevbestilling())

        resultat.harLavSatsPgaAlder.shouldBeTrue()
    }

    @Test
    fun `mapYrkesaktivPliktig - bruker 17 år gammel har ikke lav sats`() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)

        val foedselsdato = LocalDate.of(2006, 1, 1)
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)  // Alder: 17
            tom = LocalDate.of(2023, 12, 31) // Alder: 17
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        }

        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata(foedselsdato)

        val resultat = innvilgelseFtrlMapper.mapYrkesaktivPliktig(lagBrevbestilling())

        resultat.harLavSatsPgaAlder.shouldBeFalse()
    }

    @Test
    fun `mapYrkesaktivPliktig - bruker 16 år gammel har lav sats`() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)

        val foedselsdato = LocalDate.of(2007, 1, 1)
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)  // Alder: 17
            tom = LocalDate.of(2023, 12, 31) // Alder: 17
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        }

        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata(foedselsdato)

        val resultat = innvilgelseFtrlMapper.mapYrkesaktivPliktig(lagBrevbestilling())

        resultat.harLavSatsPgaAlder.shouldBeTrue()
    }

    @Test
    fun `mapYrkesaktivPliktig - bruker med forskjellig alder i perioden hvor en er utenfor intervall har lav sats`() {
        mockHappyCase(Case.paragraf_2_8)
        val behandlingsresultat = lagBehandlingsResultat(Case.paragraf_2_8)

        val foedselsdato = LocalDate.of(1953, 7, 1)
        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2023, 1, 1)  // Alder: 69
            tom = LocalDate.of(2024, 1, 1)  // Alder: 70
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            trygdedekning = Trygdedekninger.FULL_DEKNING
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        }

        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)

        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentPersondata(any()) } returns DokgenTestData.lagPersondata(foedselsdato)

        val resultat = innvilgelseFtrlMapper.mapYrkesaktivPliktig(lagBrevbestilling())

        resultat.harLavSatsPgaAlder.shouldBeTrue()
    }

    private fun lagBrevbestilling(): InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling {
        return InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling.Builder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .medForsendelseMottatt(Instant.EPOCH)
            .medBegrunnelseFritekst(BEGRUNNELSE_FRITEKST)
            .medTrygdeavgiftFritekst(TRYGDEAVGIFT_FRITEKST)
            .medSaksbehandlerNavn(SAKSBEHANDLER_NAVN)
            .medNyVurderingBakgrunn("NYE_OPPLYSNINGER")
            .build()
    }


    private fun lagBehandlingsResultat(paragraf: Case): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = lagMedlemskapsperioder(this, paragraf)
            vilkaarsresultater = setOf(Vilkaarsresultat().apply {
                vilkaar = when (paragraf) {
                    Case.paragraf_2_7 -> Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING
                    Case.paragraf_2_8 -> Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
                }
                begrunnelser = setOf(lagVilkaarBegrunnelse(this, paragraf))
            })
            nyVurderingBakgrunn = "NYE_OPPLYSNINGER"
            behandling = DokgenTestData.lagBehandling()
        }
    }

    private fun lagVilkaarBegrunnelse(vilkårsresultat: Vilkaarsresultat, paragraf: Case): VilkaarBegrunnelse =
        VilkaarBegrunnelse().apply {
            kode = when (paragraf) {
                Case.paragraf_2_7 -> Ftrl_2_7_begrunnelser.ANNEN_GRUNN.kode
                Case.paragraf_2_8 -> Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANNEN_GRUNN.kode
            }
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

    private fun lagMedlemskapsperioder(behandlingsresultat: Behandlingsresultat, paragraf: Case): List<Medlemskapsperiode> =
        listOf(Medlemskapsperiode().apply {
            fom = LocalDate.EPOCH.plusMonths(1)
            tom = LocalDate.EPOCH.plusMonths(4)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            bestemmelse = when (paragraf) {
                Case.paragraf_2_7 -> Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7_FØRSTE_LEDD
                Case.paragraf_2_8 -> Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            }
            this.behandlingsresultat = behandlingsresultat
            this.trygdeavgiftsperioder = lagTrygdeavgiftsperioder()
        })

    private fun lagTrygdeavgiftsperioder(): Set<Trygdeavgiftsperiode> {
        val inntektsperioder = listOf(lagGrunnlagInntektsperiode().apply {
            fomDato = LocalDate.EPOCH.plusMonths(1)
            tomDato = LocalDate.EPOCH.plusMonths(4)
        })
        val skatteforholdTilNorge =
            listOf(SkatteforholdTilNorge().apply { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG })


        return setOf(
            Trygdeavgiftsperiode(
                periodeFra = LocalDate.EPOCH.plusMonths(1),
                periodeTil = LocalDate.EPOCH.plusMonths(4),
                trygdesats = BigDecimal.ZERO,
                trygdeavgiftsbeløpMd = Penger(0.0),
                grunnlagInntekstperiode = inntektsperioder[0],
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge[0]
            ),
            Trygdeavgiftsperiode(
                periodeFra = LocalDate.EPOCH.plusMonths(5),
                periodeTil = LocalDate.EPOCH.plusMonths(8),
                trygdesats = BigDecimal(0.05),
                trygdeavgiftsbeløpMd = Penger(500.0),
                grunnlagInntekstperiode = inntektsperioder[0],
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge[0]
            )
        )
    }

    private fun lagGrunnlagInntektsperiode(): Inntektsperiode =
        Inntektsperiode().apply {
            type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
            isArbeidsgiversavgiftBetalesTilSkatt = true
            avgiftspliktigMndInntekt = Penger(0.0)
        }

    private fun mockHappyCase(paragraf: Case) {
        val behandlingsresultat = lagBehandlingsResultat(paragraf)
        every { mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(ofType()) } returns lagAvklarteVirksomheter()
        every { mockAvklarteVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns emptyList()
        every { mockAvklarteVirksomheterService.hentNorskeSelvstendigeForetak(ofType()) } returns emptyList()
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockDokgenMapperDatahenter.hentLandnavnFraLandkode(Landkoder.AT.kode) } returns Landkoder.AT.beskrivelse
        every { mockDokgenMapperDatahenter.hentFullmektigNavn(any(), any()) } returns null
        every { mockBehandlingsresultatService.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockAvklartUkjentSluttdatoMedlemskapsperiodeService.hentUkjentSluttdatoMedlemskapsperiode(any()) } returns true
    }

    companion object {
        const val BEGRUNNELSE_FRITEKST = "<p>Begrunnelse fritekst</p>"
        const val TRYGDEAVGIFT_FRITEKST = "<p>Trygdeavgift fritekst</p>"
        const val SAKSBEHANDLER_NAVN = "Fetter Anton"
        const val ARBEIDSGIVER_NAVN = "Bang Hansen"
        const val SAKSNUMMER = "MEL-123"

        enum class Case {
            paragraf_2_7,
            paragraf_2_8
        }
    }
}
