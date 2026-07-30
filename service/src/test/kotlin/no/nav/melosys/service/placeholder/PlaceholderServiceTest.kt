package no.nav.melosys.service.placeholder

import ch.qos.logback.classic.Level
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.service.LoggingTestUtils.withLogAppender
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
class PlaceholderServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    private lateinit var service: PlaceholderService

    @BeforeEach
    fun setup() {
        service = service(PlaceholderRegister())
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns
            Behandling.forTest {
                fagsak {
                    saksnummer = "MEL-12345"
                    medBruker()
                }
            }
    }

    @Test
    fun `katalogen inneholder alle placeholderne med eksempel`() {
        val katalog = service.hentKatalog()

        katalog.map { it.nokkel } shouldContainExactly
            listOf("saksnummer", "dagens-dato", "fornavn", "etternavn", "fodselsdato", "fodselsnummer")
        katalog.forEach { it.sakstyper.shouldBeEmpty() }
        katalog.single { it.nokkel == "saksnummer" }.eksempel() shouldBe "MEL-12345"
        katalog.single { it.nokkel == "fornavn" }.eksempel() shouldBe "Ola"
        katalog.single { it.nokkel == "etternavn" }.eksempel() shouldBe "Nordmann"
        katalog.single { it.nokkel == "fodselsdato" }.eksempel() shouldBe "15.03.2024"
        katalog.single { it.nokkel == "fodselsnummer" }.eksempel() shouldBe "12345678901"
    }

    @Test
    fun `katalogens eksempel for dagens dato er dagens faktiske dato`() {
        val eksempel = service.hentKatalog().single { it.nokkel == "dagens-dato" }.eksempel()

        eksempel shouldBeIn dagensDatoAlternativer()
    }

    @Test
    fun `henter verdier for alle felter fra behandling og persondata`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns persondata()

        val verdier = service.hentVerdier(BEHANDLING_ID).associate { it.nokkel to it.verdi }

        verdier["saksnummer"] shouldBe "MEL-12345"
        verdier["fornavn"] shouldBe "Ola"
        verdier["etternavn"] shouldBe "Nordmann"
        verdier["fodselsdato"] shouldBe "15.03.2024"
        verdier["fodselsnummer"] shouldBe "12345678901"
        verdier["dagens-dato"] shouldBeIn dagensDatoAlternativer()
    }

    @Test
    fun `persondata hentes kun en gang selv om flere felter trenger den`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns persondata()

        service.hentVerdier(BEHANDLING_ID)

        verify(exactly = 1) { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) }
    }

    @Test
    fun `persondataoppslag som feiler utelater persondatafeltene men leverer de ovrige`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } throws
            IllegalStateException("PDL feilet")

        val verdier = service.hentVerdier(BEHANDLING_ID)

        verdier.map { it.nokkel } shouldContainExactly listOf("saksnummer", "dagens-dato")
        verify(exactly = 1) { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) }
    }

    @Test
    fun `feilet persondataoppslag logges paa en linje med alle utelatte noekler`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } throws
            IllegalStateException("PDL feilet")

        withLogAppender<PlaceholderService> { appender ->
            service.hentVerdier(BEHANDLING_ID)

            appender.list.map { it.level } shouldContainExactly listOf(Level.WARN)
            appender.list.single().formattedMessage.shouldNevneAllePersondataNokler()
        }
    }

    @Test
    fun `fagsak uten bruker utelater persondatafeltene men leverer de ovrige`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns fagsakUtenBruker()

        val verdier = service.hentVerdier(BEHANDLING_ID)

        verdier.map { it.nokkel } shouldContainExactly listOf("saksnummer", "dagens-dato")
        verify(exactly = 0) { persondataFasade.hentPerson(any()) }
    }

    @Test
    fun `fagsak uten bruker er en forventet tilstand og logges som info`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns fagsakUtenBruker()

        withLogAppender<PlaceholderService> { appender ->
            service.hentVerdier(BEHANDLING_ID)

            appender.list.map { it.level } shouldContainExactly listOf(Level.INFO)
            appender.list.single().formattedMessage.shouldNevneAllePersondataNokler()
        }
    }

    private fun String.shouldNevneAllePersondataNokler() =
        listOf("fornavn", "etternavn", "fodselsdato", "fodselsnummer").forEach { this shouldContain it }

    @Test
    fun `manglende fodselsdato utelater feltet`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns
            persondata(fødselsdatoVerdi = null)

        service.hentVerdier(BEHANDLING_ID).map { it.nokkel } shouldNotContain "fodselsdato"
    }

    @Test
    fun `resolver som kaster utelater feltet uten aa velte kallet`() {
        val register = register(
            definisjon(nokkel = "kaster", resolver = { error("Oppslag feilet") }),
            definisjon(),
        )

        service(register).hentVerdier(BEHANDLING_ID) shouldContainExactly
            listOf(PlaceholderVerdi(nokkel = "saksnummer", verdi = "MEL-12345"))
    }

    @Test
    fun `resolver som gir null utelater feltet`() {
        val register = register(definisjon(nokkel = "mangler", resolver = { null }))

        service(register).hentVerdier(BEHANDLING_ID).shouldBeEmpty()
    }

    @Test
    fun `resolver som gir tom eller blank verdi utelater feltet, og verdien trimmes`() {
        val register = register(
            definisjon(nokkel = "tom", resolver = { "" }),
            definisjon(nokkel = "blank", resolver = { "   " }),
            definisjon(nokkel = "med-blanktegn", resolver = { "  Ola Nordmann \n" }),
            definisjon(),
        )

        service(register).hentVerdier(BEHANDLING_ID) shouldContainExactly listOf(
            PlaceholderVerdi(nokkel = "med-blanktegn", verdi = "Ola Nordmann"),
            PlaceholderVerdi(nokkel = "saksnummer", verdi = "MEL-12345"),
        )
    }

    @Test
    fun `katalogen gjor ingen oppslag`() {
        service.hentKatalog()

        verify(exactly = 0) { behandlingService.hentBehandling(any()) }
        verify(exactly = 0) { persondataFasade.hentPerson(any()) }
    }

    private fun service(register: PlaceholderRegister) =
        PlaceholderService(PlaceholderSakskontekstHenter(behandlingService), persondataFasade, register)

    private fun register(vararg definisjoner: PlaceholderDefinisjon): PlaceholderRegister = mockk {
        every { this@mockk.definisjoner } returns definisjoner.toList()
    }

    private fun definisjon(
        nokkel: String = "saksnummer",
        resolver: (PlaceholderKontekst) -> String? = { it.saksnummer },
    ) = PlaceholderDefinisjon(
        nokkel = nokkel,
        visningsnavn = "Visningsnavn for $nokkel",
        beskrivelse = "Beskrivelse av $nokkel",
        eksempel = { "eksempel" },
        resolver = resolver,
    )

    private fun fagsakUtenBruker(): Behandling = Behandling.forTest {
        fagsak { saksnummer = "MEL-12345" }
    }

    private fun persondata(fødselsdatoVerdi: LocalDate? = LocalDate.of(2024, 3, 15)): Persondata = mockk {
        every { fornavn } returns "Ola"
        every { etternavn } returns "Nordmann"
        every { fødselsdato } returns fødselsdatoVerdi
        every { hentFolkeregisterident() } returns "12345678901"
    }

    /** Godtar også gårsdagen, slik at et døgnskifte under testkjøringen ikke gjør testen flaky. */
    private fun dagensDatoAlternativer(): Set<String> =
        setOf(LocalDate.now(), LocalDate.now().minusDays(1))
            .map { it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
            .toSet()

    private companion object {
        const val BEHANDLING_ID = 1234L
    }
}
