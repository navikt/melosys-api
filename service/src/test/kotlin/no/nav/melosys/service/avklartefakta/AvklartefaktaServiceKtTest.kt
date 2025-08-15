package no.nav.melosys.service.avklartefakta

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Bostedsland
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.repository.AvklarteFaktaRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class AvklartefaktaServiceKtTest {

    @MockK
    private lateinit var avklarteFaktaRepository: AvklarteFaktaRepository

    @MockK
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK
    private lateinit var avklartefaktaDtoKonverterer: AvklartefaktaDtoKonverterer

    private lateinit var avklartefaktaService: AvklartefaktaService

    @BeforeEach
    fun setUp() {
        avklartefaktaService = AvklartefaktaService(
            avklarteFaktaRepository,
            behandlingsresultatRepository,
            avklartefaktaDtoKonverterer
        )
    }

    @Test
    fun `hentAvklartefakta returnerer DTO med riktige verdier`() {
        val avklartefakta = lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, "NO", "TRUE")
        val avklartefaktaSet = setOf(avklartefakta)
        every { avklarteFaktaRepository.findByBehandlingsresultatId(any()) } returns avklartefaktaSet


        val dto = avklartefaktaService.hentAlleAvklarteFakta(1L).first().shouldNotBeNull()


        dto.run {
            referanse shouldBe avklartefakta.referanse
            subjektID shouldBe avklartefakta.subjekt
            fakta shouldBe listOf(avklartefakta.fakta)
            avklartefaktaType shouldBe avklartefakta.type
            begrunnelseKoder shouldBe avklartefakta.registreringer.map { it.begrunnelseKode }
            begrunnelseFritekst shouldBe avklartefakta.begrunnelseFritekst
        }
    }

    @Test
    fun `lagreAvklarteFakta sletter og lagrer nye fakta`() {
        val behandlingsresultat = Behandlingsresultat()
        every { behandlingsresultatRepository.findById(any()) } returns Optional.of(behandlingsresultat)
        val avklartefakta = Avklartefakta().apply {
            fakta = "test fakta"
        }
        val avklartefaktaDtoer = hashSetOf(AvklartefaktaDto(avklartefakta))
        every { avklarteFaktaRepository.deleteByBehandlingsresultatId(any()) } just Runs
        every { avklarteFaktaRepository.flush() } just Runs
        every { avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(any(), any()) } returns avklartefakta
        every { avklarteFaktaRepository.saveAll(any<List<Avklartefakta>>()) } returns emptyList()


        avklartefaktaService.lagreAvklarteFakta(123L, avklartefaktaDtoer)


        verify { avklarteFaktaRepository.deleteByBehandlingsresultatId(any()) }
        verify { avklarteFaktaRepository.flush() }
        verify { avklartefaktaDtoKonverterer.opprettAvklartefaktaFraDto(any(), any()) }
        verify { avklarteFaktaRepository.saveAll(any<List<Avklartefakta>>()) }
    }

    @Test
    fun `hentAlleAvklarteArbeidsland returnerer landkoder`() {
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), eq(Avklartefaktatyper.ARBEIDSLAND))
        } returns setOf(
            lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, null, "NO"),
            lagAvklartefakta(Avklartefaktatyper.ARBEIDSLAND, null, "SE")
        )


        val landkoder = avklartefaktaService.hentAlleAvklarteArbeidsland(1L)


        landkoder shouldContainExactlyInAnyOrder listOf(Land_iso2.NO, Land_iso2.SE)
    }

    @Test
    fun `hentBostedsland returnerer bostedsland`() {
        val bostedsland = Bostedsland(Landkoder.NO)
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), eq(Avklartefaktatyper.BOSTEDSLAND))
        } returns setOf(lagAvklartefakta(Avklartefaktatyper.BOSTEDSLAND, null, bostedsland.landkode))


        val landkoder = avklartefaktaService.hentBostedland(1L)


        landkoder.shouldBePresent()
        landkoder.get() shouldBe bostedsland
    }

    @Test
    fun `hentYrkesgruppe forventer ordinær`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "ORDINAER"
        }
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(any(), any())
        } returns Optional.of(avklartefakta)


        val yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L)


        yrkesgruppeType.shouldBePresent()
        yrkesgruppeType.get() shouldBe Yrkesgrupper.ORDINAER
    }

    @Test
    fun `hentYrkesgruppe forventer flyvende`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "YRKESAKTIV_FLYVENDE"
        }
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(any(), any())
        } returns Optional.of(avklartefakta)


        val yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L)


        yrkesgruppeType.shouldBePresent()
        yrkesgruppeType.get() shouldBe Yrkesgrupper.FLYENDE_PERSONELL
    }

    @Test
    fun `hentYrkesgruppe forventer sokkel eller skip`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "SOKKEL_ELLER_SKIP"
        }
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(any(), any())
        } returns Optional.of(avklartefakta)


        val yrkesgruppeType = avklartefaktaService.finnYrkesGruppe(1L)


        yrkesgruppeType.shouldBePresent()
        yrkesgruppeType.get() shouldBe Yrkesgrupper.SOKKEL_ELLER_SKIP
    }

    @Test
    fun `hentYrkesgruppe uten yrkesgruppe forventer feil`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "IKKE_YRKESAKTIV"
        }
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(any(), any())
        } returns Optional.of(avklartefakta)


        shouldThrow<TekniskException> {
            avklartefaktaService.finnYrkesGruppe(1L)
        }.message shouldContain "Finner ingen yrkesgruppe"
    }

    @Test
    fun `hentMarginaltArbeid med ett land med marginalt arbeid gir marginalt arbeid`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "MARGINALT_ARBEID"
        }
        val avklartefaktaFraDb = setOf(avklartefakta)
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(
                any(),
                eq(Avklartefaktatyper.MARGINALT_ARBEID),
                eq("TRUE")
            )
        } returns avklartefaktaFraDb


        val harMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(1L)


        harMarginaltArbeid.shouldBeTrue()
    }

    @Test
    fun `hentMarginaltArbeid ingen land med marginalt arbeid gir ikke marginalt arbeid`() {
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(any(), any(), any())
        } returns emptySet()


        val harMarginaltArbeid = avklartefaktaService.harMarginaltArbeid(1L)


        harMarginaltArbeid.shouldBeFalse()
    }

    @Test
    fun `hentMaritimType med sokkel tekst forventer sokkel type`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "SOKKEL"
        }
        val avklartefaktaFraDb = setOf(avklartefakta)
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), any())
        } returns avklartefaktaFraDb


        val maritimTyper = avklartefaktaService.hentMaritimTyper(1L)


        maritimTyper.shouldHaveSize(1)
            .single() shouldBe Maritimtyper.SOKKEL
    }

    @Test
    fun `hentMaritimType med skip tekst forventer skip type`() {
        val avklartefakta = Avklartefakta().apply {
            fakta = "SKIP"
        }
        val avklartefaktaFraDb = setOf(avklartefakta)
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), any())
        } returns avklartefaktaFraDb


        val maritimTyper = avklartefaktaService.hentMaritimTyper(1L)


        maritimTyper.shouldHaveSize(1)
            .single() shouldBe Maritimtyper.SKIP
    }

    @Test
    fun `hentInformertMyndighet avklartFakta er Sverige forventer Sverige`() {
        val valgtMyndighetFakta = Avklartefakta().apply {
            subjekt = Landkoder.SE.kode
            type = Avklartefaktatyper.INFORMERT_MYNDIGHET
        }
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(
                any(),
                eq(Avklartefaktatyper.INFORMERT_MYNDIGHET),
                eq("TRUE")
            )
        } returns setOf(valgtMyndighetFakta)


        val result = avklartefaktaService.hentInformertMyndighet(1L)


        result.shouldBePresent()
        result.get() shouldBe Land_iso2.SE
    }

    @Test
    fun `hentAvklartMaritimeAvklartfakta med avklart sokkel gir avklart maritimt arbeid`() {
        val alleMaritimeFakta = lagAlleMaritimeAvklartefakta("Stena Don", "SOKKEL", "GB")
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(any(), any())
        } returns alleMaritimeFakta


        val avklarteMaritimeArbeid = avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(1L)


        avklarteMaritimeArbeid.values.shouldHaveSize(1).single().run {
            navn shouldBe "Stena Don"
            maritimtype shouldBe Maritimtyper.SOKKEL
            land shouldBe "GB"
        }
    }

    @Test
    fun `hentAvklartMaritimeAvklartfakta med avklart skip gir avklart maritimt arbeid`() {
        val alleMaritimeFakta = lagAlleMaritimeAvklartefakta("Stena Don", "SOKKEL", "SE") +
            lagAlleMaritimeAvklartefakta("Seven Kestrel", "SKIP", "GB")
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(any(), any())
        } returns alleMaritimeFakta


        val avklarteMaritimeArbeid = avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(1L)


        avklarteMaritimeArbeid shouldHaveSize 2
    }

    @Test
    fun `testAvklarteOrganisasjoner returnerer orgnummer`() {
        val orgnr1 = "12345678910"
        val avklartefakta = Avklartefakta().apply {
            type = Avklartefaktatyper.VIRKSOMHET
            fakta = "TRUE"
            subjekt = orgnr1
        }
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(any(), any(), eq("TRUE"))
        } returns hashSetOf(avklartefakta)


        val avklarteOrgnumre = avklartefaktaService.hentAvklarteOrgnrOgUuid(1L)


        avklarteOrgnumre shouldContainOnly listOf(orgnr1)
    }

    @Test
    fun `leggTilRegistrering forventer lagret`() {
        every {
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(any(), any<Avklartefaktatyper>())
        } returns Optional.of(Avklartefakta())
        val capturedAvklarteFakta = slot<Avklartefakta>()
        every { avklarteFaktaRepository.save(capture(capturedAvklarteFakta)) } answers { firstArg() }


        avklartefaktaService.leggTilRegistrering(1, Avklartefaktatyper.VURDERING_UNNTAK_PERIODE, "kode")


        verify { avklarteFaktaRepository.save(any()) }
        capturedAvklarteFakta.captured.registreringer shouldHaveSize 1
        val registrering = capturedAvklarteFakta.captured.registreringer.first()
        registrering.begrunnelseKode shouldBe "kode"
    }

    @Test
    fun `hentAvklarteMedfølgendeBarn med og uten medfølgende barn gir forventede verdier`() {
        val barnOmfattet = lagAvklartefakta(
            Avklartefaktatyper.VURDERING_LOVVALG_BARN,
            "omfattet", "TRUE"
        )
        val barnIkkeOmfattet1 = lagAvklartIkkeOmfattetBarn(
            "ikkeOmfattet1",
            Medfolgende_barn_begrunnelser.OVER_18_AR,
            "begrunnelseFritekst"
        )
        val barnIkkeOmfattet2 = lagAvklartIkkeOmfattetBarn(
            "ikkeOmfattet2",
            Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER,
            null
        )

        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), any<Avklartefaktatyper>())
        } returns setOf(barnOmfattet, barnIkkeOmfattet1, barnIkkeOmfattet2)


        val avklarteMedfølgendeBarn = avklartefaktaService.hentAvklarteMedfølgendeBarn(1L)


        avklarteMedfølgendeBarn.familieOmfattetAvNorskTrygd.map { it.uuid } shouldBe listOf("omfattet")

        val ikkeOmfattet = avklarteMedfølgendeBarn.familieIkkeOmfattetAvNorskTrygd
        ikkeOmfattet.map { it.uuid } shouldContainExactlyInAnyOrder listOf("ikkeOmfattet1", "ikkeOmfattet2")

        ikkeOmfattet.first { it.uuid == "ikkeOmfattet1" }.begrunnelse shouldBe Medfolgende_barn_begrunnelser.OVER_18_AR.kode
        ikkeOmfattet.first { it.uuid == "ikkeOmfattet2" }.begrunnelse shouldBe Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER.kode

        avklarteMedfølgendeBarn.hentBegrunnelseFritekst().orElse("") shouldBe "begrunnelseFritekst"
    }

    @Test
    fun `hentAvklartMedfølgendeEktefelle med medfølgende ektefelle`() {
        val ektefelleOmfattet = lagAvklartefakta(
            Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER,
            "omfattet", "TRUE"
        )
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), any<Avklartefaktatyper>())
        } returns setOf(ektefelleOmfattet)


        val avklarteMedfølgendeEktefelle = avklartefaktaService.hentAvklarteMedfølgendeEktefelle(1L)


        avklarteMedfølgendeEktefelle.familieOmfattetAvNorskTrygd.map { it.uuid } shouldBe listOf("omfattet")
    }

    @Test
    fun `hentAvklartMedfølgendeEktefelle uten medfølgende ektefelle`() {
        val ektefelleOmfattet = lagAvklartIkkeOmfattetEktefelle(
            "ikkeOmfattet",
            Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN,
            "TRUE"
        )
        every {
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(any(), any<Avklartefaktatyper>())
        } returns setOf(ektefelleOmfattet)


        val avklarteMedfølgendeEktefelle = avklartefaktaService.hentAvklarteMedfølgendeEktefelle(1L)


        val ikkeOmfattet = avklarteMedfølgendeEktefelle.familieIkkeOmfattetAvNorskTrygd.first()
        ikkeOmfattet.uuid shouldBe "ikkeOmfattet"
        ikkeOmfattet.begrunnelse shouldBe Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN.name
    }

    fun lagAlleMaritimeAvklartefakta(navn: String, maritimType: String, landkode: String): Set<Avklartefakta> = hashSetOf(
        AvklartMaritimtArbeidTest.lagAvklartefaktaSokkelSkip(navn, maritimType),
        AvklartMaritimtArbeidTest.lagAvklartefaktaArbeidsland(navn, landkode)
    )

    private fun lagAvklartIkkeOmfattetBarn(
        subjectID: String,
        begrunnelse: Medfolgende_barn_begrunnelser,
        begrunnelseFritekst: String?
    ) = lagAvklartefakta(Avklartefaktatyper.VURDERING_LOVVALG_BARN, subjectID, "FALSE").also {
        it.begrunnelseFritekst = begrunnelseFritekst
        it.registreringer = setOf(AvklartefaktaRegistrering().apply<AvklartefaktaRegistrering> {
            this.begrunnelseKode = begrunnelse.kode
        })
    }

    private fun lagAvklartIkkeOmfattetEktefelle(
        subjectID: String,
        begrunnelse: Medfolgende_ektefelle_samboer_begrunnelser_ftrl,
        begrunnelseFritekst: String
    ) = lagAvklartefakta(
        Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER,
        subjectID,
        "FALSE"
    ).also {
        it.begrunnelseFritekst = begrunnelseFritekst
        it.registreringer = setOf(AvklartefaktaRegistrering().apply {
            this.begrunnelseKode = begrunnelse.kode
        })
    }

    private fun lagAvklartefakta(type: Avklartefaktatyper, subjektID: String?, fakta: String) = Avklartefakta().apply {
        this.referanse = "Referanse"
        this.subjekt = subjektID
        this.fakta = fakta
        this.type = type
        this.begrunnelseFritekst = "Fritekst"
    }
}
