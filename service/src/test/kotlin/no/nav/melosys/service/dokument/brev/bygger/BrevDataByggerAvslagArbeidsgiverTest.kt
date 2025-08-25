package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.Vilkaar.VESENTLIG_VIRKSOMHET
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Vesentlig_virksomhet_begrunnelser
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.MottatteOpplysningerStub.lagMottatteOpplysninger
import no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysninger
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagPersonsaksopplysning
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BrevDataByggerAvslagArbeidsgiverTest {
    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @MockK
    private lateinit var vilkaarsresultatService: VilkaarsresultatService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    private lateinit var brevDataByggerAvslagArbeidsgiver: BrevDataByggerAvslagArbeidsgiver

    @BeforeEach
    fun setUp() {
        every { landvelgerService.hentArbeidsland(any()) } returns Land_iso2.AT

        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()

        brevDataByggerAvslagArbeidsgiver = BrevDataByggerAvslagArbeidsgiver(
            landvelgerService,
            lovvalgsperiodeService,
            vilkaarsresultatService
        )
    }

    @Test
    fun `avslagsbrev arbeidsgiver skal inneholde hovedvirksomhet med riktig orgnummer`() {
        val behandling = Behandling.forTest {
            id = 1L
            fagsak {
                medBruker()
            }
        }
        behandling.saksopplysninger.add(lagPersonsaksopplysning(PersonDokument()))

        val personDokument = PersonDokument().apply {
            sammensattNavn = "Navn Navnesen"
        }
        val person = Saksopplysning().apply {
            dokument = personDokument
            type = SaksopplysningType.PERSOPL
        }

        val saksopplysninger = lagArbeidsforholdOpplysninger(listOf("123456789"))
        saksopplysninger.add(person)
        behandling.saksopplysninger = saksopplysninger

        behandling.mottatteOpplysninger = lagMottatteOpplysninger(
            emptyList(),
            emptyList(),
            listOf("987654321")
        )

        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.DE
            fom = LocalDate.now()
            tom = LocalDate.now()
        }
        every { lovvalgsperiodeService.hentLovvalgsperiode(behandling.id) } returns lovvalgsperiode

        val orgSet = setOf("987654321")
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(behandling.id) } returns orgSet

        val organisasjonsDetaljer = mockk<OrganisasjonsDetaljer>()
        every { organisasjonsDetaljer.hentStrukturertForretningsadresse() } returns lagStrukturertAdresse()
        every { organisasjonsDetaljer.opphoersdato } returns null
        val organisasjonDokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer("987654321")
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()

        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns emptySet()
        every { organisasjonOppslagService.hentOrganisasjoner(orgSet) } returns setOf(organisasjonDokument)

        val vilkaarsresultatArt121 =
            lagVilkårresultat(Vilkaar.FO_883_2004_ART12_1, Utsendt_arbeidstaker_begrunnelser.IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER.kode)
        val vesentligVirksomhet = lagVilkårresultat(Vilkaar.VESENTLIG_VIRKSOMHET, Vesentlig_virksomhet_begrunnelser.FOR_LITE_KONTRAKTER_NORGE.kode)

        every { vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(any()) } returns vilkaarsresultatArt121
        every { vilkaarsresultatService.finnVilkaarsresultat(any(), eq(VESENTLIG_VIRKSOMHET)) } returns vesentligVirksomhet

        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            mockk<BehandlingService>(),
            kodeverkService
        )
        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val dataGrunnlag = BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, persondata)
        val saksbehandler = "saksbehandler"


        val brevData = brevDataByggerAvslagArbeidsgiver.lag(dataGrunnlag, saksbehandler) as BrevDataAvslagArbeidsgiver


        brevData.hovedvirksomhet?.orgnr shouldBe "987654321"
    }

    private fun lagVilkårresultat(vilkaarType: Vilkaar, vilkårbegrunnelseKode: String): Vilkaarsresultat {
        val begrunnelser = VilkaarBegrunnelse().apply {
            kode = vilkårbegrunnelseKode
        }
        return Vilkaarsresultat().apply {
            setOppfylt(false)
            vilkaar = vilkaarType
            this.begrunnelser = setOf(begrunnelser)
        }
    }
}
