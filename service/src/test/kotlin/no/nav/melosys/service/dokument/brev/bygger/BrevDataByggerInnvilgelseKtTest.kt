package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.optional.shouldNotBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarInnvilgelse
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class BrevDataByggerInnvilgelseKtTest {
    
    private val avklartefaktaService: AvklartefaktaService = mockk()
    private val avklarteVirksomheterService: AvklarteVirksomheterService = mockk()
    private val kodeverkService: KodeverkService = mockk()
    private val lovvalgsperiodeService: LovvalgsperiodeService = mockk()
    private val landvelgerService: LandvelgerService = mockk()
    private val brevDataByggerA1: BrevDataByggerA1 = mockk()
    private val anmodningsperiodeService: AnmodningsperiodeService = mockk()
    private val vilkaarsresultatService: VilkaarsresultatService = mockk()
    private val persondataFasade: PersondataFasade = mockk()
    private val mottatteOpplysningerService: MottatteOpplysningerService = mockk()
    
    private lateinit var behandling: Behandling
    private lateinit var brevbestillingDto: BrevbestillingDto
    private lateinit var brevDataByggerInnvilgelse: BrevDataByggerInnvilgelse
    
    companion object {
        private const val SAKSBEHANDLER = "saksbehandler"
    }
    
    @BeforeEach
    fun setUp() {
        val fagsak = FagsakTestFactory.builder().medBruker().build()
        
        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(MottatteOpplysninger())
            .build()
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData = Soeknad()
        
        brevbestillingDto = BrevbestillingDto().apply {
            mottaker = Mottakerroller.BRUKER
            begrunnelseKode = "BEGRUNNELSEKODE"
            fritekst = "FRITEKST"
        }
        
        val person = PersonDokument().apply {
            sammensattNavn = "Tom Mestokk"
        }
        behandling.saksopplysninger.add(lagPersonsaksopplysning(person))
        
        every { brevDataByggerA1.lag(any(), any()) } returns BrevDataA1()
        
        val virksomhet = AvklartVirksomhet("Bedrift AS", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID)
        every { avklarteVirksomheterService.hentAlleNorskeVirksomheter(any()) } returns listOf(virksomhet)
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()
        
        val periode = Lovvalgsperiode()
        every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns periode
        
        every { landvelgerService.hentArbeidsland(any()) } returns Land_iso2.AT
        every { landvelgerService.hentBostedsland(any(), any<MottatteOpplysningerData>()) } returns Bostedsland(Landkoder.NO)
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(any()) } returns listOf(Land_iso2.DE)
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns AvklarteMedfolgendeFamilie(emptySet(), emptySet())
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()
        every { avklartefaktaService.hentMaritimTyper(any()) } returns emptySet()
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns emptyList()
        every { vilkaarsresultatService.harVilkaarForUnntak(any()) } returns false
        every { vilkaarsresultatService.harVilkaarForUtsending(any()) } returns false
        every { vilkaarsresultatService.oppfyllerVilkaar(any(), any()) } returns false
        
        brevDataByggerInnvilgelse = BrevDataByggerInnvilgelse(
            avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingDto,
            brevDataByggerA1,
            vilkaarsresultatService,
            persondataFasade,
            mottatteOpplysningerService
        )
    }
    
    private fun lagBrevdataGrunnlag(): BrevDataGrunnlag {
        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        return BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
    }
    
    @Test
    fun `lag medSokkel setterMaritimtypeSokkel`() {
        val maritimType = Maritimtyper.SOKKEL
        every { avklartefaktaService.hentMaritimTyper(any()) } returns setOf(maritimType)
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.saksbehandler shouldBe SAKSBEHANDLER
        brevData.avklartMaritimType shouldBe Maritimtyper.SOKKEL
    }
    
    @Test
    fun `lag utenMaritimtArbeid setterMaritimtypeTilNull`() {
        every { avklartefaktaService.hentMaritimTyper(any()) } returns emptySet()
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.avklartMaritimType shouldBe null
    }
    
    @Test
    fun `lag medFtrl2_12 setterTuristSkipTrue`() {
        every { vilkaarsresultatService.oppfyllerVilkaar(behandling.id, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.turistskip shouldBe true
    }
    
    @Test
    fun `lag innvilgelsesBrev harBestillingsinformasjon`() {
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER)
        brevData.begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
        brevData.fritekst shouldBe brevbestillingDto.fritekst
        brevData.saksbehandler shouldBe SAKSBEHANDLER
    }
    
    @Test
    fun `lag medAnmodningsperiode girAnmodningsperiodeSvar`() {
        val anmodningsperiode = Anmodningsperiode().apply {
            setSendtUtland(true)
            anmodningsperiodeSvar = lagAnmodningsperiodeSvarInnvilgelse()
        }
        
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns listOf(anmodningsperiode)
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.getAnmodningsperiodesvar().shouldBePresent()
        brevData.getAnmodningsperiodesvar().get() shouldBe anmodningsperiode.anmodningsperiodeSvar
    }
    
    @Test
    fun `lag utenAnmodningsperiode erMulig`() {
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns emptyList()
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.getAnmodningsperiodesvar().shouldNotBePresent()
    }
    
    @Test
    fun `lag erArt12 art16UtenArt12False`() {
        every { vilkaarsresultatService.harVilkaarForUtsending(any()) } returns true
        every { vilkaarsresultatService.harVilkaarForUnntak(any()) } returns true
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.art16UtenArt12 shouldBe false
    }
    
    @Test
    fun `lag erArt16UtenArt12 art16UtenArt12True`() {
        every { vilkaarsresultatService.harVilkaarForUtsending(any()) } returns false
        every { vilkaarsresultatService.harVilkaarForUnntak(any()) } returns true
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        brevData.art16UtenArt12 shouldBe true
    }
    
    @Test
    fun `lag medfølgendeBarnHarFnr henterNavnFraTps`() {
        val barn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), "fnr1", null, MedfolgendeFamilie.Relasjonsrolle.BARN)
        val barn2 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), "fnr2", null, MedfolgendeFamilie.Relasjonsrolle.BARN)
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            personOpplysninger.medfolgendeFamilie = listOf(barn1, barn2)
        }
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }
        
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns AvklarteMedfolgendeFamilie(
            setOf(OmfattetFamilie(barn1.uuid)),
            setOf(IkkeOmfattetFamilie(barn2.uuid, null, null))
        )
        every { mottatteOpplysningerService.hentMottatteOpplysninger(any()) } returns mottatteOpplysninger
        every { persondataFasade.hentSammensattNavn(barn1.fnr) } returns "Navn1"
        every { persondataFasade.hentSammensattNavn(barn2.fnr) } returns "Navn2"
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        
        brevData.avklarteMedfolgendeBarn!!.familieOmfattetAvNorskTrygd shouldHaveSize 1
        brevData.avklarteMedfolgendeBarn!!.familieOmfattetAvNorskTrygd.first().apply {
            sammensattNavn shouldBe "Navn1"
            ident shouldBe barn1.fnr
        }
        
        brevData.avklarteMedfolgendeBarn!!.familieIkkeOmfattetAvNorskTrygd shouldHaveSize 1
        brevData.avklarteMedfolgendeBarn!!.familieIkkeOmfattetAvNorskTrygd.first().sammensattNavn shouldBe "Navn2"
        
        verify(exactly = 2) { persondataFasade.hentSammensattNavn(any()) }
    }
    
    @Test
    fun `lag medfølgendeBarnHarUuid henterNavnFraMottatteOpplysninger`() {
        val barn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn1", MedfolgendeFamilie.Relasjonsrolle.BARN)
        val barn2 = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn2", MedfolgendeFamilie.Relasjonsrolle.BARN)
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            personOpplysninger.medfolgendeFamilie = listOf(barn1, barn2)
        }
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }
        
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns AvklarteMedfolgendeFamilie(
            setOf(OmfattetFamilie(barn1.uuid)),
            setOf(IkkeOmfattetFamilie(barn2.uuid, null, null))
        )
        every { mottatteOpplysningerService.hentMottatteOpplysninger(any()) } returns mottatteOpplysninger
        
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevdataGrunnlag(), SAKSBEHANDLER) as BrevDataInnvilgelse
        
        brevData.avklarteMedfolgendeBarn!!.familieOmfattetAvNorskTrygd.map { it.sammensattNavn } shouldContainExactly listOf(barn1.navn)
        brevData.avklarteMedfolgendeBarn!!.familieIkkeOmfattetAvNorskTrygd.map { it.sammensattNavn } shouldContainExactly listOf(barn2.navn)
        
        verify(exactly = 0) { persondataFasade.hentSammensattNavn(any()) }
    }
    
    @Test
    fun `lag omfattetBarnIkkeIMottatteOpplysninger kasterException`() {
        val barn = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn", MedfolgendeFamilie.Relasjonsrolle.BARN)
        val brevDataGrunnlag = lagBrevdataGrunnlag()
        
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns AvklarteMedfolgendeFamilie(
            setOf(OmfattetFamilie(barn.uuid)),
            emptySet()
        )
        every { mottatteOpplysningerService.hentMottatteOpplysninger(any()) } returns MottatteOpplysninger()
        
        val exception = shouldThrow<FunksjonellException> {
            brevDataByggerInnvilgelse.lag(brevDataGrunnlag, SAKSBEHANDLER)
        }
        exception.message shouldContain "finnes ikke i mottatteOpplysningeret"
    }
    
    @Test
    fun `lag ikkeOmfattetBarnIkkeIMottatteOpplysninger kasterException`() {
        val barn = MedfolgendeFamilie.tilMedfolgendeFamilie(UUID.randomUUID().toString(), null, "Navn", MedfolgendeFamilie.Relasjonsrolle.BARN)
        val brevDataGrunnlag = lagBrevdataGrunnlag()
        
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns AvklarteMedfolgendeFamilie(
            emptySet(),
            setOf(IkkeOmfattetFamilie(barn.uuid, null, null))
        )
        every { mottatteOpplysningerService.hentMottatteOpplysninger(any()) } returns MottatteOpplysninger()
        
        val exception = shouldThrow<FunksjonellException> {
            brevDataByggerInnvilgelse.lag(brevDataGrunnlag, SAKSBEHANDLER)
        }
        exception.message shouldContain "finnes ikke i mottatteOpplysningeret"
    }
}