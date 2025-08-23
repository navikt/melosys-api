package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.KORT_OPPDRAG_RETUR_NORSK_AG
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_naeringsdrivende_begrunnelser.UTSENDELSE_OVER_24_MN
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger
import no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrevDataByggerAnmodningUnntakTest {

    private val avklartefaktaService: AvklartefaktaService = mockk(relaxed = true)
    private val organisasjonOppslagService: OrganisasjonOppslagService = mockk(relaxed = true)
    private val vilkaarsresultatService: VilkaarsresultatService = mockk(relaxed = true)
    private val landvelgerService: LandvelgerService = mockk(relaxed = true)
    private val kodeverkService: KodeverkService = mockk(relaxed = true)

    private lateinit var brevDataByggerAnmodningUnntak: BrevDataByggerAnmodningUnntak

    @BeforeEach
    fun setUp() {
        brevDataByggerAnmodningUnntak = BrevDataByggerAnmodningUnntak(landvelgerService, vilkaarsresultatService)

        every { vilkaarsresultatService.harVilkaarForUtsending(any()) } answers { callOriginal() }
        every { vilkaarsresultatService.finnUnntaksVilkaarsresultat(any()) } returns
            BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, KORT_OPPDRAG_RETUR_NORSK_AG)

        // Set up default behavior for utsending methods to return null (no article 12 conditions)
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns null
        every { vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(any()) } returns null
    }

    @Test
    fun `lag annmodningUnntakBrev avklarVirksomhetSomSelvstendigForetak`() {
        val behandling = lagBehandling()

        val brevData = brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), SAKSBEHANDLER) as BrevDataAnmodningUnntak
        brevData.run {
            hovedvirksomhet!!.orgnr shouldBe "999"
            hovedvirksomhet!!.erSelvstendigForetak() shouldBe true
            arbeidsland shouldBe Landkoder.DE.beskrivelse
        }
    }

    @Test
    fun `lag brevDataUtenArt12 girAnmodningUtenArt12Begrunnelser`() {
        val behandling = lagBehandling()
        val brevData = brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), SAKSBEHANDLER) as BrevDataAnmodningUnntak
        brevData.run {
            anmodningBegrunnelser.shouldBeEmpty()
            anmodningUtenArt12Begrunnelser.shouldNotBeEmpty()
        }
    }

    @Test
    fun `lag brevDataMedArt121 girAnmodningBegrunnelser`() {
        every { vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(any()) } returns
            BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false, UTSENDELSE_OVER_24_MN)

        val behandling = lagBehandling()
        val brevData = brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), SAKSBEHANDLER) as BrevDataAnmodningUnntak
        brevData.run {
            anmodningBegrunnelser.shouldNotBeEmpty()
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
        }
    }

    @Test
    fun `lag brevDataMedArt122 girAnmodningBegrunnelser`() {
        every { vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(any()) } returns
            BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false, UTSENDELSE_OVER_24_MN)

        val behandling = lagBehandling()
        val brevData = brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), SAKSBEHANDLER) as BrevDataAnmodningUnntak
        brevData.run {
            anmodningBegrunnelser.shouldNotBeEmpty()
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
        }
    }

    @Test
    fun `lag brevDataMedOppfyltArt121 girAnmodningBegrunnelser`() {
        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns
            BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, true)

        val behandling = lagBehandling()
        val brevData = brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), SAKSBEHANDLER) as BrevDataAnmodningUnntak
        brevData.run {
            anmodningBegrunnelser.shouldNotBeEmpty()
            anmodningUtenArt12Begrunnelser.shouldBeEmpty()
        }
    }

    @Test
    fun `lag brevDataMedFritekst`() {
        val vilkaar = BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, KORT_OPPDRAG_RETUR_NORSK_AG).apply {
            begrunnelseFritekst = "FRITEKST"
        }
        every { vilkaarsresultatService.finnUnntaksVilkaarsresultat(any()) } returns vilkaar

        val behandling = lagBehandling()
        val brevData = brevDataByggerAnmodningUnntak.lag(lagBrevressurser(behandling), SAKSBEHANDLER) as BrevDataAnmodningUnntak
        brevData.anmodningFritekst shouldBe "FRITEKST"
    }

    private fun lagBehandling(): Behandling {
        val behandling = Behandling.forTest {
            id = 1L
            fagsak = FagsakTestFactory.lagFagsak()
        }

        val selvstendigeForetak = listOf("987654321")
        val arbeidsgivereRegister = listOf("123456789")

        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, emptyList(), emptyList())

        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister)
        behandling.saksopplysninger = saksopplysninger
        behandling.saksopplysninger.add(BrevDataTestUtils.lagPersonsaksopplysning(PersonDokument()))

        return behandling
    }

    private fun lagBrevressurser(behandling: Behandling): BrevDataGrunnlag {
        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            mockk<BehandlingService>(),
            kodeverkService
        )

        val orgSet = setOf("987654321")
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.id) } returns orgSet

        every { landvelgerService.hentArbeidsland(any()) } returns Land_iso2.DE

        val organisasjonsDetaljer = mockk<OrganisasjonsDetaljer> {
            every { hentStrukturertForretningsadresse() } returns BrevDataTestUtils.lagStrukturertAdresse()
        }

        val organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer("999")
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()

        every { organisasjonOppslagService.hentOrganisasjoner(orgSet) } returns setOf(organisasjonDokument)

        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()

        return BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
    }

    companion object {
        private const val SAKSBEHANDLER = "saksbehandler"
    }
}
