package no.nav.melosys.service.avklartefakta

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.Avklartefakta.Companion.IKKE_VALGT_FAKTA
import no.nav.melosys.domain.avklartefakta.Avklartefakta.Companion.VALGT_FAKTA
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.IKKE_SOEKERS_BARN
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.IKKE_TRE_AV_FEM_SISTE_ÅR
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AvklarteFaktaRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarteMedfolgendeFamilieServiceKtTest {
    
    @MockK
    private lateinit var avklarteFaktaRepository: AvklarteFaktaRepository
    
    @MockK
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository
    
    @MockK
    private lateinit var behandlingService: BehandlingService
    
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    
    @MockK
    private lateinit var avklartefaktaDtoKonverterer: AvklartefaktaDtoKonverterer
    
    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService
    
    private lateinit var avklarteMedfolgendeFamilieService: AvklarteMedfolgendeFamilieService
    
    companion object {
        private const val UUID_BARN = "uuidBarn"
        private const val UUID_BARN2 = "uuidBarn2"
        private const val UUID_EKTEFELLE_SAMBOER = "uuidEktefelleSamboer"
        private const val FRITEKST_BARN = "fritekstBarn"
        private const val FRITEKST_EKTEFELLE_SAMBOER = "fritekstEktefelleSamboer"
    }
    
    @BeforeEach
    fun setUp() {
        val avklartefaktaService = AvklartefaktaService(
            avklarteFaktaRepository, 
            behandlingsresultatRepository, 
            avklartefaktaDtoKonverterer
        )
        avklarteMedfolgendeFamilieService = AvklarteMedfolgendeFamilieService(
            behandlingService, 
            behandlingsresultatService, 
            avklartefaktaService, 
            mottatteOpplysningerService
        )
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta ikke omfattet familie lagres korrekt`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = emptySet(),
            familieIkkeOmfattetAvNorskTrygd = setOf(
                IkkeOmfattetFamilie(UUID_BARN, OVER_18_AR.kode, FRITEKST_BARN),
                IkkeOmfattetFamilie(UUID_EKTEFELLE_SAMBOER, SAMBOER_UTEN_FELLES_BARN.kode, FRITEKST_EKTEFELLE_SAMBOER)
            )
        )
        
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
        }
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        
        val capturedAvklarteFakta = mutableListOf<Avklartefakta>()
        every { avklarteFaktaRepository.save(capture(capturedAvklarteFakta)) } answers { firstArg() }
        
        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        
        verify(exactly = 2) { avklarteFaktaRepository.save(any()) }
        
        capturedAvklarteFakta shouldHaveSize 2
        capturedAvklarteFakta.sortBy { it.subjekt }
        
        val avklartBarn = capturedAvklarteFakta[0]
        val avklartEktefelleSamboer = capturedAvklarteFakta[1]
        
        avklartBarn.subjekt shouldBe UUID_BARN
        avklartBarn.type shouldBe Avklartefaktatyper.VURDERING_LOVVALG_BARN
        avklartBarn.referanse shouldBe Avklartefaktatyper.VURDERING_LOVVALG_BARN.kode
        avklartBarn.fakta shouldBe IKKE_VALGT_FAKTA
        avklartBarn.begrunnelseFritekst shouldBe FRITEKST_BARN
        avklartBarn.behandlingsresultat shouldBe behandlingsresultat
        avklartBarn.registreringer shouldHaveSize 1
        
        val registreringBarn = avklartBarn.registreringer.first()
        registreringBarn.begrunnelseKode shouldBe OVER_18_AR.kode
        
        avklartEktefelleSamboer.subjekt shouldBe UUID_EKTEFELLE_SAMBOER
        avklartEktefelleSamboer.type shouldBe Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER
        avklartEktefelleSamboer.referanse shouldBe Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.kode
        avklartEktefelleSamboer.fakta shouldBe IKKE_VALGT_FAKTA
        avklartEktefelleSamboer.begrunnelseFritekst shouldBe FRITEKST_EKTEFELLE_SAMBOER
        avklartEktefelleSamboer.behandlingsresultat shouldBe behandlingsresultat
        avklartEktefelleSamboer.registreringer shouldHaveSize 1
        
        val registreringEktefelleSamboer = avklartEktefelleSamboer.registreringer.first()
        registreringEktefelleSamboer.begrunnelseKode shouldBe SAMBOER_UTEN_FELLES_BARN.kode
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta omfattet familie lagres korrekt`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = setOf(
                OmfattetFamilie(UUID_BARN), 
                OmfattetFamilie(UUID_EKTEFELLE_SAMBOER)
            ),
            familieIkkeOmfattetAvNorskTrygd = emptySet()
        )
        
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
        }
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        
        val capturedAvklarteFakta = mutableListOf<Avklartefakta>()
        every { avklarteFaktaRepository.save(capture(capturedAvklarteFakta)) } answers { firstArg() }
        
        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        
        verify(exactly = 2) { avklarteFaktaRepository.save(any()) }
        
        capturedAvklarteFakta shouldHaveSize 2
        capturedAvklarteFakta.sortBy { it.subjekt }
        
        val avklartBarn = capturedAvklarteFakta[0]
        val avklartEktefelleSamboer = capturedAvklarteFakta[1]
        
        avklartBarn.subjekt shouldBe UUID_BARN
        avklartBarn.type shouldBe Avklartefaktatyper.VURDERING_LOVVALG_BARN
        avklartBarn.referanse shouldBe Avklartefaktatyper.VURDERING_LOVVALG_BARN.kode
        avklartBarn.fakta shouldBe VALGT_FAKTA
        avklartBarn.begrunnelseFritekst.shouldBeNull()
        avklartBarn.behandlingsresultat shouldBe behandlingsresultat
        avklartBarn.registreringer.shouldBeEmpty()
        
        avklartEktefelleSamboer.subjekt shouldBe UUID_EKTEFELLE_SAMBOER
        avklartEktefelleSamboer.type shouldBe Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER
        avklartEktefelleSamboer.referanse shouldBe Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.kode
        avklartEktefelleSamboer.fakta shouldBe VALGT_FAKTA
        avklartEktefelleSamboer.begrunnelseFritekst.shouldBeNull()
        avklartEktefelleSamboer.behandlingsresultat shouldBe behandlingsresultat
        avklartEktefelleSamboer.registreringer.shouldBeEmpty()
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta omfattet familie ikke lagret i mottatte opplysninger kaster feilmelding`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = setOf(OmfattetFamilie("uuid3")),
            familieIkkeOmfattetAvNorskTrygd = emptySet()
        )
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        }.message shouldContain "Medfolgende familie som er omfattet av norsk trygd: uuid3 er ikke lagret i mottatteOpplysningeret."
        
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta ikke omfattet familie ikke lagret i mottatte opplysninger kaster feilmelding`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = emptySet(),
            familieIkkeOmfattetAvNorskTrygd = setOf(
                IkkeOmfattetFamilie("uuid3", OVER_18_AR.kode, FRITEKST_BARN)
            )
        )
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        }.message shouldContain "Medfolgende familie som ikke er omfattet av norsk trygd: uuid3 er ikke lagret i mottatteOpplysningeret."
        
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta ugyldig begrunnelse kode for barn kaster feilmelding`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = emptySet(),
            familieIkkeOmfattetAvNorskTrygd = setOf(
                IkkeOmfattetFamilie(UUID_BARN, SAMBOER_UTEN_FELLES_BARN.kode, FRITEKST_BARN)
            )
        )
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        }.message shouldContain "Begrunnelsen til medfolgende barn $UUID_BARN: ${SAMBOER_UTEN_FELLES_BARN.kode} er ikke gyldig."
        
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta ugyldig begrunnelse kode for ektefelle samboer kaster feilmelding`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = emptySet(),
            familieIkkeOmfattetAvNorskTrygd = setOf(
                IkkeOmfattetFamilie(UUID_EKTEFELLE_SAMBOER, OVER_18_AR.kode, FRITEKST_EKTEFELLE_SAMBOER)
            )
        )
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        }.message shouldContain "Begrunnelsen til medfolgende ektefelle/samboer $UUID_EKTEFELLE_SAMBOER: ${OVER_18_AR.kode} er ikke gyldig."
        
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }
    
    @Test
    fun `lagreMedfolgendeFamilieSomAvklartefakta ikke satt begrunnelse kode kaster feilmelding`() {
        val avklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(
            familieOmfattetAvNorskTrygd = emptySet(),
            familieIkkeOmfattetAvNorskTrygd = setOf(
                IkkeOmfattetFamilie(UUID_BARN, null, FRITEKST_BARN)
            )
        )
        
        every { behandlingService.hentBehandling(1L) } returns mockBehandling()
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie)
        }.message shouldContain "Begrunnelsen til medfolgende familie $UUID_BARN: null er ikke satt."
        
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }
    
    @Test
    fun `hentAvklarteMedfølgendeBarn medfølgende ikke satt i mottatte opplysninger kaster feil`() {
        every { 
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(1L, Avklartefaktatyper.VURDERING_LOVVALG_BARN) 
        } returns setOf(lagAvklartMedfølgendeBarn(UUID_BARN))
        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns lagMottatteOpplysninger(false)
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(1L)
        }.message shouldContain "Avklart medfølgende barn $UUID_BARN finnes ikke i mottatteOpplysningeret"
    }
    
    @Test
    fun `hentAvklarteMedfølgendeBarn ikke medfølgende ikke satt i mottatte opplysninger kaster feil`() {
        every { 
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(1L, Avklartefaktatyper.VURDERING_LOVVALG_BARN) 
        } returns setOf(lagAvklartIkkeMedfølgendeBarn(UUID_BARN2))
        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns lagMottatteOpplysninger(false)
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(1L)
        }.message shouldContain "Avklart medfølgende barn $UUID_BARN2 finnes ikke i mottatteOpplysningeret"
    }
    
    @Test
    fun `hentAvklarteMedfølgendeBarn returnerer medfølgende barn`() {
        every { 
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(1L, Avklartefaktatyper.VURDERING_LOVVALG_BARN) 
        } returns setOf(lagAvklartMedfølgendeBarn(UUID_BARN), lagAvklartIkkeMedfølgendeBarn(UUID_BARN2))
        
        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns lagMottatteOpplysninger()
        
        val avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(1L)
        
        avklarteMedfolgendeBarn.familieIkkeOmfattetAvNorskTrygd shouldHaveSize 1
        avklarteMedfolgendeBarn.familieIkkeOmfattetAvNorskTrygd.map { it.uuid } shouldContainExactly listOf(UUID_BARN2)
        
        avklarteMedfolgendeBarn.familieOmfattetAvNorskTrygd shouldHaveSize 1
        avklarteMedfolgendeBarn.familieOmfattetAvNorskTrygd.map { it.uuid } shouldContainExactly listOf(UUID_BARN)
    }
    
    @Test
    fun `hentAvklarteMedfølgendeEktefelle medfølgende ikke satt i mottatte opplysninger kaster feil`() {
        every { 
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(1L, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER) 
        } returns setOf(lagAvklartMedfølgendeEktefelle(UUID_EKTEFELLE_SAMBOER))
        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns lagMottatteOpplysninger(false)
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(1L)
        }.message shouldContain "Avklart medfølgende ektefelle/samboer $UUID_EKTEFELLE_SAMBOER finnes ikke i mottatteOpplysningeret"
    }
    
    @Test
    fun `hentAvklarteMedfølgendeEktefelle ikke medfølgende ikke satt i mottatte opplysninger kaster feil`() {
        every { 
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(1L, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER) 
        } returns setOf(lagAvklartIkkeMedfølgendeEktefelle(UUID_EKTEFELLE_SAMBOER))
        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns lagMottatteOpplysninger(false)
        
        shouldThrow<FunksjonellException> {
            avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(1L)
        }.message shouldContain "Avklart medfølgende ektefelle/samboer $UUID_EKTEFELLE_SAMBOER finnes ikke i mottatteOpplysningeret"
    }
    
    @Test
    fun `hentAvklarteMedfølgendeEktefelle returnerer medfølgende ektefelle`() {
        every { 
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(1L, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER) 
        } returns setOf(lagAvklartMedfølgendeEktefelle(UUID_EKTEFELLE_SAMBOER))
        
        every { mottatteOpplysningerService.hentMottatteOpplysninger(1L) } returns lagMottatteOpplysninger()
        
        val avklarteMedfolgendeFamilie = avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(1L)
        
        val omfattet = avklarteMedfolgendeFamilie.familieOmfattetAvNorskTrygd
        val ikkeOmfattet = avklarteMedfolgendeFamilie.familieIkkeOmfattetAvNorskTrygd
        
        omfattet shouldHaveSize 1
        ikkeOmfattet.shouldBeEmpty()
        
        omfattet.first().uuid shouldBe UUID_EKTEFELLE_SAMBOER
    }
    
    private fun lagMottatteOpplysninger(medFolgendeFamilie: Boolean = true): MottatteOpplysninger {
        val mottatteOpplysningerData = MottatteOpplysningerData()
        if (medFolgendeFamilie) {
            mottatteOpplysningerData.personOpplysninger.medfolgendeFamilie = listOf(
                MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN, "09080723451", "Bjartmar", MedfolgendeFamilie.Relasjonsrolle.BARN),
                MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN2, "09080723452", "Arnhild", MedfolgendeFamilie.Relasjonsrolle.BARN),
                MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_EKTEFELLE_SAMBOER, "09080656743", "Kari", MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER)
            )
        } else {
            mottatteOpplysningerData.personOpplysninger.medfolgendeFamilie = emptyList()
        }
        return MottatteOpplysninger().apply {
            mottatteOpplysningerData = mottatteOpplysningerData
        }
    }
    
    private fun lagAvklartMedfølgendeBarn(uuid: String): Avklartefakta {
        return lagAvklartFakta(uuid, Avklartefaktatyper.VURDERING_LOVVALG_BARN, VALGT_FAKTA)
    }
    
    private fun lagAvklartIkkeMedfølgendeBarn(uuid: String): Avklartefakta {
        val avklartefakta = lagAvklartFakta(uuid, Avklartefaktatyper.VURDERING_LOVVALG_BARN, IKKE_VALGT_FAKTA)
        avklartefakta.begrunnelseFritekst = IKKE_SOEKERS_BARN.kode
        val avklartefaktaRegistrering = AvklartefaktaRegistrering().apply {
            begrunnelseKode = IKKE_SOEKERS_BARN.kode
        }
        avklartefakta.registreringer = setOf(avklartefaktaRegistrering)
        return avklartefakta
    }
    
    private fun lagAvklartMedfølgendeEktefelle(uuid: String): Avklartefakta {
        return lagAvklartFakta(uuid, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, VALGT_FAKTA)
    }
    
    private fun lagAvklartIkkeMedfølgendeEktefelle(uuid: String): Avklartefakta {
        val avklartefakta = lagAvklartFakta(uuid, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, IKKE_VALGT_FAKTA)
        avklartefakta.begrunnelseFritekst = IKKE_TRE_AV_FEM_SISTE_ÅR.kode
        val avklartefaktaRegistrering = AvklartefaktaRegistrering().apply {
            begrunnelseKode = IKKE_TRE_AV_FEM_SISTE_ÅR.kode
        }
        avklartefakta.registreringer = setOf(avklartefaktaRegistrering)
        return avklartefakta
    }
    
    private fun lagAvklartFakta(uuid: String, avklartefaktatype: Avklartefaktatyper, valgtFakta: String): Avklartefakta {
        return Avklartefakta(null, null, avklartefaktatype, uuid, valgtFakta)
    }
    
    private fun mockBehandling(): Behandling {
        val medfolgendeFamilieUuid1 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN, "fnr1", null, MedfolgendeFamilie.Relasjonsrolle.BARN)
        val medfolgendeFamilieUuid2 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_EKTEFELLE_SAMBOER, "fnr2", null, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER)
        val mottatteOpplysningerData = MottatteOpplysningerData()
        mottatteOpplysningerData.personOpplysninger.medfolgendeFamilie.addAll(listOf(medfolgendeFamilieUuid1, medfolgendeFamilieUuid2))
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }
        return BehandlingTestFactory.builderWithDefaults()
            .medMottatteOpplysninger(mottatteOpplysninger)
            .build()
    }
}