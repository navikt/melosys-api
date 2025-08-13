package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.KORT_OPPDRAG_RETUR_NORSK_AG
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger
import no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrevDataByggerAvslagYrkesaktivKtTest {
    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @MockK
    private lateinit var vilkaarsresultatService: VilkaarsresultatService

    private lateinit var brevDataByggerAvslagYrkesaktiv: BrevDataByggerAvslagYrkesaktiv
    private lateinit var anmodningsperiodeSvar: AnmodningsperiodeSvar

    @BeforeEach
    fun setUp() {
        anmodningsperiodeSvar = lagAnmodningsperiodeSvarAvslag()
        val anmodningsperiode = Anmodningsperiode().apply {
            anmodningsperiodeSvar = this@BrevDataByggerAvslagYrkesaktivKtTest.anmodningsperiodeSvar
            setSendtUtland(true)
        }
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns listOf(anmodningsperiode)

        every { vilkaarsresultatService.finnUnntaksVilkaarsresultat(any()) } returns lagVilkaarsresultat(
            Vilkaar.FO_883_2004_ART16_1,
            true,
            KORT_OPPDRAG_RETUR_NORSK_AG
        )

        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns emptySet()

        brevDataByggerAvslagYrkesaktiv =
            BrevDataByggerAvslagYrkesaktiv(landvelgerService, anmodningsperiodeService, BrevbestillingDto(), vilkaarsresultatService)
    }

    @Test
    fun `lag_annmodningUnntakBrev_avklarVirksomhetSomSelvstendigForetak`() {
        val behandling = Behandling.forTest {
            id = 1L
            fagsak {
                medBruker()
            }
        }

        val selvstendigeForetak = listOf("987654321")
        val arbeidsgivereRegister = listOf("123456789")

        val saksopplysninger = lagArbeidsforholdOpplysninger(arbeidsgivereRegister)
        behandling.saksopplysninger = saksopplysninger
        behandling.saksopplysninger.add(lagPersonsaksopplysning(PersonDokument()))
        behandling.mottatteOpplysninger = lagMottatteOpplysninger(selvstendigeForetak, emptyList(), emptyList())

        val orgSet = setOf("987654321")
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.id) } returns orgSet

        every { landvelgerService.hentArbeidsland(any()) } returns Land_iso2.DE
        val organisasjonsDetaljer = mockk<OrganisasjonsDetaljer> {
            every { hentStrukturertForretningsadresse() } returns lagStrukturertAdresse()
        }
        val organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer("999")
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()

        every { organisasjonOppslagService.hentOrganisasjoner(orgSet) } returns setOf(organisasjonDokument)
        every { vilkaarsresultatService.harVilkaarForUtsending(any()) } returns false
        every { vilkaarsresultatService.harVilkaarForUnntak(any()) } returns true

        val saksbehandler = "saksbehandler"


        val brevData = brevDataByggerAvslagYrkesaktiv.lag(lagBrevressurser(behandling), saksbehandler) as BrevDataAvslagYrkesaktiv


        brevData.run {
            hovedvirksomhet?.orgnr shouldBe "999"
            hovedvirksomhet?.erSelvstendigForetak() shouldBe true
            arbeidsland shouldBe Landkoder.DE.beskrivelse
            anmodningsperiodeSvar shouldBe anmodningsperiodeSvar
            art16UtenArt12 shouldBe true
        }
    }

    private fun lagAnmodningsperiodeSvarAvslag(): AnmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
        begrunnelseFritekst = "No tiendo"
        anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
    }

    private fun lagBrevressurser(behandling: Behandling): BrevDataGrunnlag {
        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService, mockk<BehandlingService>(), kodeverkService
        )
        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        return BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
    }
}
