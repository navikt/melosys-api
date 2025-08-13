package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.SaksopplysningStubs.lagArbeidsforholdOpplysning
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BrevDataByggerA1KtTest {
    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    private lateinit var avklarteOrganisasjoner: MutableSet<String>
    private lateinit var søknad: Soeknad
    private lateinit var dataGrunnlag: BrevDataGrunnlag
    private lateinit var brevDataByggerA1: BrevDataByggerA1

    private val saksbehandler = ""
    private val orgnr2 = "10987654321"

    @BeforeEach
    fun setUp() {
        val behandling = Behandling.forTest {
            id = 123L
            fagsak {
                medBruker()
            }
        }
        val fagsak = behandling.fagsak

        avklarteOrganisasjoner = mutableSetOf()

        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns avklarteOrganisasjoner
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap<String, no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid>()
        every { avklartefaktaService.finnYrkesGruppe(any()) } returns Optional.empty()
        every { landvelgerService.hentAlleArbeidsland(any()) } returns emptyList()

        val oppgittAdresse = lagStrukturertAdresse()
        søknad = Soeknad()
        søknad.bosted.oppgittAdresse = oppgittAdresse

        val foretakUtland = ForetakUtland()
        foretakUtland.orgnr = "12345678910"
        foretakUtland.navn = "Utenlandsk arbeidsgiver AS"
        søknad.foretakUtland.add(foretakUtland)

        behandling.mottatteOpplysninger = MottatteOpplysninger()
        behandling.mottatteOpplysninger?.mottatteOpplysningerData = søknad

        val personDok = PersonDokument()
        val person = Saksopplysning().apply {
            setDokument(personDok)
            setType(SaksopplysningType.PERSOPL)
        }

        val arbeidsforhold = lagArbeidsforholdOpplysning(listOf(orgnr2))
        behandling.saksopplysninger = mutableSetOf(person, arbeidsforhold)

        val kodeverkService = mockk<KodeverkService>()
        every { kodeverkService.dekod(any(), any()) } returns "Oslo"

        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            mockk<BehandlingService>(),
            kodeverkService
        )
        val brevbestilling = DoksysBrevbestilling.Builder().medBehandling(behandling).build()
        dataGrunnlag = BrevDataGrunnlag(brevbestilling, kodeverkService, avklarteVirksomheterService, avklartefaktaService, personDok)
        brevDataByggerA1 = BrevDataByggerA1(avklartefaktaService, landvelgerService)
    }

    private fun mockAvklarteOrganisasjoner(orgnumre: List<String>) {
        avklarteOrganisasjoner.addAll(orgnumre)
        val detaljer = mockk<OrganisasjonsDetaljer>()
        every { detaljer.hentStrukturertForretningsadresse() } returns lagStrukturertAdresse()
        every { detaljer.opphoersdato } returns null

        val organisasjonDokumenter = mutableSetOf<OrganisasjonDokument>()
        for (orgnr in orgnumre) {
            organisasjonDokumenter.add(leggTilTestorganisasjon("navn$orgnr", orgnr, detaljer))
        }

        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns organisasjonDokumenter
    }

    private fun leggTilTestorganisasjon(navn: String, orgnummer: String, detaljer: OrganisasjonsDetaljer): OrganisasjonDokument {
        val org = OrganisasjonDokumentTestFactory.builder()
            .orgnummer(orgnummer)
            .navn(navn)
            .organisasjonsDetaljer(detaljer)
            .build()
        val saksopplysning = Saksopplysning().apply {
            setType(SaksopplysningType.ORG)
            setDokument(org)
        }
        return org
    }

    @Test
    fun `lag brukAlleArbeidsland`() {
        mockAvklarteOrganisasjoner(listOf("1"))
        brevDataByggerA1.lag(dataGrunnlag, saksbehandler)

        verify { landvelgerService.hentAlleArbeidsland(any()) }
    }

    @Test
    fun `lag sjekkAvklarteSelvstendigeForetak`() {
        mockAvklarteOrganisasjoner(listOf("999"))
        val foretak = SelvstendigForetak()
        foretak.orgnr = "999"
        søknad.selvstendigArbeid.selvstendigForetak = søknad.selvstendigArbeid.selvstendigForetak + foretak

        val brevDataDto = brevDataByggerA1.lag(dataGrunnlag, saksbehandler) as BrevDataA1
        brevDataDto.hovedvirksomhet?.orgnr shouldBe foretak.orgnr
    }

    @Test
    fun `lag hentAvklarteArbeidsgivere`() {
        mockAvklarteOrganisasjoner(listOf("7777"))
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere + "7777"

        val brevDataDto = brevDataByggerA1.lag(dataGrunnlag, saksbehandler) as BrevDataA1
        brevDataDto.hovedvirksomhet?.orgnr shouldBe "7777"
    }

    private fun lagStrukturertAdresse(): StrukturertAdresse = StrukturertAdresse().apply {
        gatenavn = "HjemmeGata"
        husnummerEtasjeLeilighet = "23B"
        postnummer = "0165"
        poststed = "Oslo"
        landkode = Landkoder.NO.kode
    }

    @Test
    fun `lag ArbeidsstedHosOppdragsgiver girUtenlandskvirksomhet`() {
        mockAvklarteOrganisasjoner(listOf(orgnr2))

        val fysiskArbeidssted = FysiskArbeidssted("Utenlandsk Oppdragsgiver LTD", lagStrukturertAdresse())
        søknad.arbeidPaaLand.fysiskeArbeidssteder = søknad.arbeidPaaLand.fysiskeArbeidssteder + fysiskArbeidssted

        val brevDataDto = brevDataByggerA1.lag(dataGrunnlag, saksbehandler) as BrevDataA1
        brevDataDto.run {
            bivirksomheter?.map { it.navn }?.shouldContain(fysiskArbeidssted.virksomhetNavn)
            arbeidssteder?.filter { it.erFysisk() }
                ?.map { it as no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted }
                ?.map { it.adresse }?.shouldContain(fysiskArbeidssted.adresse)
        }
    }
}
