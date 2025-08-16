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
class BrevDataByggerInnvilgelseFlereLandTest {
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
        behandling = Behandling.forTest {
            id = 1L
            fagsak {
                medBruker()
            }
            saksopplysninger.add(lagPersonsopplysning())
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = Soeknad()
            }
        }

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

        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()

        every { avklarteVirksomheterService.hentNorskeArbeidsgivere(any()) } returns emptyList()

        every { avklarteVirksomheterService.hentUtenlandskeVirksomheter(any()) } returns emptyList()

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

    private fun lagBrevressurser(): BrevDataGrunnlag =
        BrevDataGrunnlag(
            DoksysBrevbestilling.Builder().medBehandling(behandling).build(),
            null,
            avklarteVirksomheterService,
            avklartefaktaService,
            PersonopplysningerObjectFactory.lagPersonopplysninger()
        )

    private fun lagPersonsopplysning(): Saksopplysning =
        lagPersonsaksopplysning(PersonDokument())

    @Test
    fun `lag med sokkel skal sette maritimtype sokkel`() {
        val maritimType = Maritimtyper.SOKKEL
        every { avklartefaktaService.hentMaritimTyper(any()) } returns setOf(maritimType)


        val brevdataressurser = lagBrevressurser()
        val brevData = brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler) as BrevDataInnvilgelseFlereLand


        brevData.run {
            saksbehandler shouldBe this@BrevDataByggerInnvilgelseFlereLandTest.saksbehandler
            avklartMaritimTypeSokkel shouldBe true
            avklartMaritimTypeSkip shouldBe false
        }
    }

    @Test
    fun `lag uten maritimt arbeid skal sette maritimtype til null`() {
        every { avklartefaktaService.hentMaritimTyper(any()) } returns emptySet()


        val brevdataressurser = lagBrevressurser()
        val brevData = brevDataByggerInnvilgelse.lag(brevdataressurser, saksbehandler) as BrevDataInnvilgelseFlereLand


        brevData.run {
            avklartMaritimTypeSokkel shouldBe false
            avklartMaritimTypeSkip shouldBe false
            trydemyndighetsland shouldBe null
        }
    }

    @Test
    fun `lag utpeking annet land skal sette trydemyndighetsland`() {
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
    fun `lag innvilgelsesbrev skal ha bestillingsinformasjon`() {
        val brevData = brevDataByggerInnvilgelse.lag(lagBrevressurser(), saksbehandler)


        brevData.run {
            begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
            fritekst shouldBe brevbestillingDto.fritekst
            saksbehandler shouldBe this@BrevDataByggerInnvilgelseFlereLandTest.saksbehandler
        }
    }
}
