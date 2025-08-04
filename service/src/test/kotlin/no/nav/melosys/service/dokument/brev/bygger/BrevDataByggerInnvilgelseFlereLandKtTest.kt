package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrevDataByggerInnvilgelseFlereLandKtTest {
    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var saksopplysningerService: SaksopplysningerService

    @MockK
    private lateinit var brevDataByggerA1: BrevDataByggerA1

    private lateinit var behandling: Behandling
    private lateinit var brevbestillingDto: BrevbestillingDto
    private val saksbehandler = "saksbehandler"
    private lateinit var brevDataByggerInnvilgelse: BrevDataBygger

    @BeforeEach
    fun setUp() {
        val fagsak = FagsakTestFactory.builder().medBruker().build()

        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(fagsak)
            .build()

        behandling.saksopplysninger.add(lagPersonsopplysning())
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.mottatteOpplysningerData = Soeknad()
        behandling.mottatteOpplysninger = mottatteOpplysninger

        brevbestillingDto = BrevbestillingDto().apply {
            mottaker = Mottakerroller.BRUKER
            begrunnelseKode = "BEGRUNNELSEKODE"
            fritekst = "FRITEKST"
        }

        every { brevDataByggerA1.lag(any(), any()) } returns BrevDataA1()

        val periode = Lovvalgsperiode()
        every { lovvalgsperiodeService.hentLovvalgsperiode(any()) } returns periode

        every { landvelgerService.hentAlleArbeidsland(any()) } returns setOf(Land_iso2.AT)
        every { landvelgerService.hentBostedsland(any(), any()) } returns Bostedsland(Landkoder.DE)
        every { landvelgerService.isFlereLandUkjentHvilke(any()) } returns false

        // Add missing mock setup for avklartefaktaService
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()

        // Add missing mock setup for avklarteVirksomheterService
        every { avklarteVirksomheterService.hentNorskeArbeidsgivere(any()) } returns emptyList()

        // Add missing mock setup for avklarteVirksomheterService.hentUtenlandskeVirksomheter
        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

        // Add missing mock setups for avklartefaktaService methods
        every { avklartefaktaService.hentMaritimTyper(any()) } returns emptySet()
        every { avklartefaktaService.harMarginaltArbeid(any()) } returns false

        brevDataByggerInnvilgelse = BrevDataByggerInnvilgelseFlereLand(
            avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            saksopplysningerService,
            brevbestillingDto,
            brevDataByggerA1
        )
    }

    private fun lagBrevressurser(): BrevDataGrunnlag {
        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        return BrevDataGrunnlag(brevbestilling, null, avklarteVirksomheterService, avklartefaktaService, persondata)
    }

    private fun lagPersonsopplysning(): Saksopplysning {
        val person = PersonDokument()
        return lagPersonsaksopplysning(person)
    }

    @Test
    fun lag_medSokkel_setterMaritimtypeSokkel() {
        val maritimType = Maritimtyper.SOKKEL
        every { avklartefaktaService.hentMaritimTyper(any()) } returns setOf(maritimType)

        val brevdataressurser = lagBrevressurser()
        val brevData = brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler) as BrevDataInnvilgelseFlereLand
        brevData.saksbehandler shouldBe saksbehandler
        brevData.avklartMaritimTypeSokkel shouldBe true
        brevData.avklartMaritimTypeSkip shouldBe false
    }

    @Test
    fun lag_utenMaritimtArbeid_setterMaritimtypeTilNull() {
        every { avklartefaktaService.hentMaritimTyper(any()) } returns emptySet()

        val brevdataressurser = lagBrevressurser()
        val brevData = brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler) as BrevDataInnvilgelseFlereLand
        brevData.avklartMaritimTypeSokkel shouldBe false
        brevData.avklartMaritimTypeSkip shouldBe false
        brevData.trydemyndighetsland shouldBe null
    }

    @Test
    fun lag_utpekingAnnetLand_setterTrydemyndighetsland() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        val sedDokument = SedDokument().apply {
            avsenderLandkode = Landkoder.DE
        }
        every { saksopplysningerService.hentSedOpplysninger(behandling.id) } returns sedDokument

        val brevdataressurser = lagBrevressurser()
        val brevData = brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler) as BrevDataInnvilgelseFlereLand
        brevData.trydemyndighetsland shouldBe Landkoder.DE
    }

    @Test
    fun lag_innvilgelsesBrev_harBestillingsinformasjon() {
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevressurser(), saksbehandler)

        brevData.begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
        brevData.fritekst shouldBe brevbestillingDto.fritekst
        brevData.saksbehandler shouldBe saksbehandler
    }
}
