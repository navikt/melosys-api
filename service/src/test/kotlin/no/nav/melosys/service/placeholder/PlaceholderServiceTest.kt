package no.nav.melosys.service.placeholder

import ch.qos.logback.classic.Level
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.person.Foedsel
import no.nav.melosys.domain.person.Folkeregisteridentifikator
import no.nav.melosys.domain.person.KjoennType
import no.nav.melosys.domain.person.Master
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.service.LoggingTestUtils.withLogAppender
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
class PlaceholderServiceTest {

    @MockK
    private lateinit var sakskontekstHenter: PlaceholderSakskontekstHenter

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockK
    private lateinit var kodeverkService: KodeverkService

    private lateinit var service: PlaceholderService

    @BeforeEach
    fun setup() {
        service = service(PlaceholderRegister())
        medSakskontekst()
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns persondata()
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO") } returns "Norge"
    }

    @Test
    fun `katalogen inneholder alle placeholderne med eksempel`() {
        val katalog = service.hentKatalog().placeholdere

        katalog.map { it.nokkel } shouldContainExactly listOf(
            "saksnummer", "dagens-dato", "fornavn", "etternavn", "fodselsdato", "fodselsnummer",
            "lovvalgsperiode-fra", "lovvalgsperiode-til", "medlemskapsperiode-fra", "medlemskapsperiode-til",
            "soknadsperiode-fra", "soknadsperiode-til", "bostedsadresse", "bosted-postnummer", "bosted-poststed",
            "oppholdsadresse", "kontaktadresse", "arbeidsgiver-norge", "arbeidsgiver-utland", "arbeidsland",
        )
        katalog.forEach { it.eksempel().shouldNotBeBlank() }
        katalog.single { it.nokkel == "saksnummer" }.eksempel() shouldBe "MEL-12345"
        katalog.single { it.nokkel == "fodselsdato" }.eksempel() shouldBe "15.03.2024"
    }

    @Test
    fun `bare lovvalgsperiodene er avgrenset til sakstyper med lovvalg`() {
        val katalog = service.hentKatalog().placeholdere

        katalog.filter { it.sakstyper.isNotEmpty() }.map { it.nokkel } shouldContainExactly
            listOf("lovvalgsperiode-fra", "lovvalgsperiode-til")
        katalog.single { it.nokkel == "lovvalgsperiode-fra" }.sakstyper.map { it.name } shouldContainExactly
            listOf("EU_EOS", "TRYGDEAVTALE")
    }

    @Test
    fun `katalogens eksempel for dagens dato er dagens faktiske dato`() {
        val eksempel = service.hentKatalog().placeholdere.single { it.nokkel == "dagens-dato" }.eksempel()

        eksempel shouldBeIn dagensDatoAlternativer()
    }

    @Test
    fun `henter verdier for alle felter fra behandling og persondata`() {
        val verdier = verdier()

        verdier["saksnummer"]?.verdi shouldBe "MEL-12345"
        verdier["fornavn"]?.verdi shouldBe "Ola"
        verdier["etternavn"]?.verdi shouldBe "Nordmann"
        verdier["fodselsdato"]?.verdi shouldBe "15.03.2024"
        verdier["fodselsnummer"]?.verdi shouldBe "12345678901"
        verdier["dagens-dato"]?.verdi shouldBeIn dagensDatoAlternativer()
    }

    @Test
    fun `persondata hentes kun en gang selv om flere felter trenger den`() {
        service.hentVerdier(BEHANDLING_ID)

        verify(exactly = 1) { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) }
    }

    @Test
    fun `persondataoppslag som feiler utelater persondatafeltene men leverer de ovrige`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } throws
            IllegalStateException("PDL feilet")

        val verdier = service.hentVerdier(BEHANDLING_ID).verdier

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
        medSakskontekst(sakskontekst(brukersAktørID = null))

        val verdier = service.hentVerdier(BEHANDLING_ID).verdier

        verdier.map { it.nokkel } shouldContainExactly listOf("saksnummer", "dagens-dato")
        verify(exactly = 0) { persondataFasade.hentPerson(any()) }
    }

    @Test
    fun `fagsak uten bruker er en forventet tilstand og logges som info`() {
        medSakskontekst(sakskontekst(brukersAktørID = null))

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

        service.hentVerdier(BEHANDLING_ID).verdier.map { it.nokkel } shouldNotContain "fodselsdato"
    }

    @Test
    fun `lovvalgsperioden er alltid entydig og faar aldri kandidater`() {
        medSakskontekst(
            sakskontekst(
                lovvalgsperiode = PeriodeData(dato(1, 3, 2024), dato(28, 2, 2027)),
                avgiftspliktigPerioder = listOf(
                    innvilget(dato(1, 3, 2024), dato(28, 2, 2027)),
                    innvilget(dato(1, 1, 2023), dato(29, 2, 2024)),
                ),
            )
        )

        val verdier = verdier()

        verdier["lovvalgsperiode-fra"] shouldBe PlaceholderVerdi("lovvalgsperiode-fra", "01.03.2024")
        verdier["lovvalgsperiode-til"] shouldBe PlaceholderVerdi("lovvalgsperiode-til", "28.02.2027")
    }

    @Test
    fun `medlemskapsperioden forhaandsvelger ytterpunktene og tilbyr enkeltperiodene`() {
        medSakskontekst(
            sakskontekst(
                medlemskapsperiodeFom = dato(1, 1, 2023),
                medlemskapsperiodeTom = dato(28, 2, 2027),
                avgiftspliktigPerioder = listOf(
                    innvilget(dato(1, 3, 2024), dato(28, 2, 2027)),
                    innvilget(dato(1, 1, 2023), dato(29, 2, 2024)),
                    PeriodeData(dato(1, 1, 2020), dato(31, 12, 2020), erInnvilget = false),
                ),
            )
        )

        val verdier = verdier()

        verdier["medlemskapsperiode-fra"] shouldBe PlaceholderVerdi(
            nokkel = "medlemskapsperiode-fra",
            verdi = "01.01.2023",
            kandidater = listOf("01.01.2023", "01.03.2024"),
        )
        verdier["medlemskapsperiode-til"] shouldBe PlaceholderVerdi(
            nokkel = "medlemskapsperiode-til",
            verdi = "28.02.2027",
            kandidater = listOf("28.02.2027", "29.02.2024"),
        )
    }

    @Test
    fun `lovvalgssaker utelater medlemskapsnoklene, som ellers ville duplisert lovvalgsperioden`() {
        medSakskontekst(
            sakskontekst(
                erLovvalg = true,
                lovvalgsperiode = PeriodeData(dato(1, 3, 2024), dato(28, 2, 2027)),
                medlemskapsperiodeFom = dato(1, 3, 2024),
                medlemskapsperiodeTom = dato(28, 2, 2027),
                avgiftspliktigPerioder = listOf(innvilget(dato(1, 3, 2024), dato(28, 2, 2027))),
            )
        )

        val verdier = verdier()

        verdier["lovvalgsperiode-fra"]?.verdi shouldBe "01.03.2024"
        verdier.keys shouldNotContain "medlemskapsperiode-fra"
        verdier.keys shouldNotContain "medlemskapsperiode-til"
    }

    @Test
    fun `en enkelt medlemskapsperiode gir ingen kandidater`() {
        medSakskontekst(
            sakskontekst(
                medlemskapsperiodeFom = dato(1, 3, 2024),
                avgiftspliktigPerioder = listOf(innvilget(dato(1, 3, 2024))),
            )
        )

        verdier()["medlemskapsperiode-fra"] shouldBe PlaceholderVerdi("medlemskapsperiode-fra", "01.03.2024")
    }

    @Test
    fun `apen sluttdato utelater til-feltet`() {
        medSakskontekst(
            sakskontekst(
                lovvalgsperiode = PeriodeData(dato(1, 3, 2024), null),
                medlemskapsperiodeFom = dato(1, 3, 2024),
                avgiftspliktigPerioder = listOf(innvilget(dato(1, 3, 2024))),
            )
        )

        val verdier = verdier()

        verdier["lovvalgsperiode-fra"]?.verdi shouldBe "01.03.2024"
        verdier.keys shouldNotContain "lovvalgsperiode-til"
        verdier.keys shouldNotContain "medlemskapsperiode-til"
    }

    @Test
    fun `soknadsperioden tilbyr den samlede utsendingsperioden som alternativ`() {
        medSakskontekst(
            sakskontekst(
                soknadsperioder = listOf(
                    PeriodeData(dato(1, 3, 2024), dato(28, 2, 2027)),
                    PeriodeData(dato(1, 4, 2024), dato(31, 3, 2027)),
                ),
            )
        )

        val verdier = verdier()

        verdier["soknadsperiode-fra"] shouldBe PlaceholderVerdi(
            nokkel = "soknadsperiode-fra",
            verdi = "01.03.2024",
            kandidater = listOf("01.03.2024", "01.04.2024"),
        )
        verdier["soknadsperiode-til"]?.kandidater shouldContainExactly listOf("28.02.2027", "31.03.2027")
    }

    @Test
    fun `apen forhaandsvalgt soknadsperiode utelater til-feltet i stedet for aa laane sluttdato fra alternativet`() {
        medSakskontekst(
            sakskontekst(
                soknadsperioder = listOf(
                    PeriodeData(dato(1, 3, 2024), null),
                    PeriodeData(dato(1, 4, 2024), dato(31, 3, 2027)),
                ),
            )
        )

        val verdier = verdier()

        verdier["soknadsperiode-fra"] shouldBe PlaceholderVerdi(
            nokkel = "soknadsperiode-fra",
            verdi = "01.03.2024",
            kandidater = listOf("01.03.2024", "01.04.2024"),
        )
        verdier.keys shouldNotContain "soknadsperiode-til"
    }

    @Test
    fun `bostedsadressen leveres som en linje med postnummer og poststed hver for seg`() {
        val verdier = verdier()

        verdier["bostedsadresse"] shouldBe PlaceholderVerdi("bostedsadresse", "Storgata 1, 0155, Oslo, Norge")
        verdier["bosted-postnummer"] shouldBe PlaceholderVerdi("bosted-postnummer", "0155")
        verdier["bosted-poststed"] shouldBe PlaceholderVerdi("bosted-poststed", "Oslo")
    }

    @Test
    fun `ukjent landkode gir adresselinje uten land i stedet for feil`() {
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "XX") } returns KodeverkService.UKJENT
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER, "XX") } returns KodeverkService.UKJENT
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns
            persondata(bostedsadresse = bostedsadresse(landkode = "XX"))

        verdier()["bostedsadresse"] shouldBe PlaceholderVerdi("bostedsadresse", "Storgata 1, 0155, Oslo")
    }

    @Test
    fun `oppholdsadressene sorteres nyeste forst og taaler manglende registrertDato`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns persondata(
            oppholdsadresser = listOf(
                oppholdsadresse("Uten dato", registrertDato = null),
                oppholdsadresse("Eldst", registrertDato = LocalDateTime.of(2020, 1, 1, 0, 0)),
                oppholdsadresse("Nyest", registrertDato = LocalDateTime.of(2026, 1, 1, 0, 0)),
                oppholdsadresse("Historisk", registrertDato = LocalDateTime.of(2027, 1, 1, 0, 0), erHistorisk = true),
            )
        )

        val oppholdsadresse = verdier().getValue("oppholdsadresse")

        oppholdsadresse.verdi shouldBe "Nyest 1, 0155, Oslo, Norge"
        oppholdsadresse.kandidater shouldContainExactly listOf(
            "Nyest 1, 0155, Oslo, Norge",
            "Eldst 1, 0155, Oslo, Norge",
            "Uten dato 1, 0155, Oslo, Norge",
        )
    }

    @Test
    fun `ugyldig adresse er ikke kandidat`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns persondata(
            oppholdsadresser = listOf(
                oppholdsadresse("Gyldig", registrertDato = LocalDateTime.of(2020, 1, 1, 0, 0)),
                // Norsk adresse uten postnummer er utgått eller ufullstendig i PDL – erGyldig() er false
                oppholdsadresse("Uten postnummer", registrertDato = LocalDateTime.of(2026, 1, 1, 0, 0), postnummer = null),
            )
        )

        verdier()["oppholdsadresse"] shouldBe PlaceholderVerdi("oppholdsadresse", "Gyldig 1, 0155, Oslo, Norge")
    }

    @Test
    fun `c-o-adressenavnet kommer forst i adresselinjen`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns
            persondata(bostedsadresse = bostedsadresse(coAdressenavn = "c/o Kari Nordmann"))

        verdier()["bostedsadresse"] shouldBe
            PlaceholderVerdi("bostedsadresse", "c/o Kari Nordmann, Storgata 1, 0155, Oslo, Norge")
    }

    @Test
    fun `kontaktadressen bygges ogsaa fra en semistrukturert adresse`() {
        every { persondataFasade.hentPerson(FagsakTestFactory.BRUKER_AKTØR_ID) } returns persondata(
            kontaktadresser = listOf(
                kontaktadresse(
                    strukturertAdresse = strukturertAdresse(),
                    registrertDato = LocalDateTime.of(2026, 1, 1, 0, 0),
                ),
                kontaktadresse(
                    semistrukturertAdresse = SemistrukturertAdresse("Postboks 5", null, null, null, "0028", "Oslo", "NO"),
                    registrertDato = LocalDateTime.of(2020, 1, 1, 0, 0),
                ),
            )
        )

        verdier().getValue("kontaktadresse").kandidater shouldContainExactly listOf(
            "Storgata 1, 0155, Oslo, Norge",
            "Postboks 5, 0028, Oslo, Norge",
        )
    }

    @Test
    fun `norske arbeidsgivernavn hentes fra EREG en gang og alle navn er kandidater`() {
        medSakskontekst(sakskontekst(norskeArbeidsgivereOrgnumre = setOf("123456789", "987654321")))
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns setOf(
            OrganisasjonDokumentTestFactory.builder().orgnummer("123456789").navn("Nordisk Verksted AS").build(),
            OrganisasjonDokumentTestFactory.builder().orgnummer("987654321").navn("Bergen Mekaniske AS").build(),
        )

        verdier()["arbeidsgiver-norge"] shouldBe PlaceholderVerdi(
            nokkel = "arbeidsgiver-norge",
            verdi = "Bergen Mekaniske AS",
            kandidater = listOf("Bergen Mekaniske AS", "Nordisk Verksted AS"),
        )
        verify(exactly = 1) { organisasjonOppslagService.hentOrganisasjoner(setOf("123456789", "987654321")) }
    }

    @Test
    fun `EREG kalles ikke naar ingen noekkel i registeret trenger arbeidsgivernavn`() {
        medSakskontekst(sakskontekst(norskeArbeidsgivereOrgnumre = setOf("123456789")))

        service(register(definisjon())).hentVerdier(BEHANDLING_ID)

        verify(exactly = 0) { organisasjonOppslagService.hentOrganisasjoner(any()) }
    }

    @Test
    fun `utenlandske arbeidsgivere og arbeidsland kommer fra sakskonteksten`() {
        medSakskontekst(
            sakskontekst(
                arbeidsland = listOf("Tyskland"),
                utenlandskeArbeidsgivere = listOf("Nordwerk GmbH", "Alpin AG"),
            )
        )

        val verdier = verdier()

        verdier["arbeidsgiver-utland"]?.kandidater shouldContainExactly listOf("Nordwerk GmbH", "Alpin AG")
        verdier["arbeidsland"] shouldBe PlaceholderVerdi("arbeidsland", "Tyskland")
    }

    @Test
    fun `resolver som kaster utelater feltet uten aa velte kallet`() {
        val register = register(
            definisjon(nokkel = "kaster", resolver = { error("Oppslag feilet") }),
            definisjon(),
        )

        service(register).hentVerdier(BEHANDLING_ID).verdier shouldContainExactly
            listOf(PlaceholderVerdi(nokkel = "saksnummer", verdi = "MEL-12345"))
    }

    @Test
    fun `resolver som gir null utelater feltet`() {
        val register = register(definisjon(nokkel = "mangler", resolver = { null }))

        service(register).hentVerdier(BEHANDLING_ID).verdier.shouldBeEmpty()
    }

    @Test
    fun `resolver som gir tom eller blank verdi utelater feltet, og verdien trimmes`() {
        val register = register(
            definisjon(nokkel = "tom", resolver = { PlaceholderResultat("") }),
            definisjon(nokkel = "blank", resolver = { PlaceholderResultat("   ") }),
            definisjon(nokkel = "med-blanktegn", resolver = { PlaceholderResultat("  Ola Nordmann \n") }),
            definisjon(),
        )

        service(register).hentVerdier(BEHANDLING_ID).verdier shouldContainExactly listOf(
            PlaceholderVerdi(nokkel = "med-blanktegn", verdi = "Ola Nordmann"),
            PlaceholderVerdi(nokkel = "saksnummer", verdi = "MEL-12345"),
        )
    }

    @Test
    fun `kandidater trimmes, og tomme og duplikater fjernes`() {
        val register = register(
            definisjon(nokkel = "duplikat", resolver = { PlaceholderResultat("Oslo", listOf(" Oslo ", "", "Oslo", "  ")) }),
            definisjon(nokkel = "flere", resolver = { PlaceholderResultat("Oslo", listOf("Oslo", " Bergen ")) }),
        )

        service(register).hentVerdier(BEHANDLING_ID).verdier shouldContainExactly listOf(
            PlaceholderVerdi(nokkel = "duplikat", verdi = "Oslo"),
            PlaceholderVerdi(nokkel = "flere", verdi = "Oslo", kandidater = listOf("Oslo", "Bergen")),
        )
    }

    @Test
    fun `katalogen gjor ingen oppslag`() {
        service.hentKatalog()

        verify(exactly = 0) { sakskontekstHenter.hent(any()) }
        verify(exactly = 0) { persondataFasade.hentPerson(any()) }
        verify(exactly = 0) { organisasjonOppslagService.hentOrganisasjoner(any()) }
    }

    @Test
    fun `katalogen inneholder alle betingelsene med visningsnavn og beskrivelse`() {
        val betingelser = service.hentKatalog().betingelser

        betingelser.map { it.nokkel } shouldContainExactly listOf(
            "innvilgelse", "avslag", "opphort", "delvis-innvilgelse", "apen-sluttdato", "skattepliktig",
            "har-lonn-fra-norge", "har-inntekt-fra-utlandet", "trygdeavgift-til-skatt", "utsending",
            "pensjonist", "forstegangsvurdering", "ny-vurdering",
        )
        betingelser.forEach {
            it.visningsnavn.shouldNotBeBlank()
            it.beskrivelse.shouldNotBeBlank()
        }
    }

    // Nøklene skrives rått i tekstblokkene og må tåle samme mønster som placeholdernøklene
    @Test
    fun `betingelsesnoklene folger nokkelmonsteret og kolliderer ikke med placeholdernoklene`() {
        val katalog = service.hentKatalog()

        katalog.betingelser.forEach { it.nokkel shouldMatch Regex("^[a-z0-9-]+$") }
        katalog.betingelser.map { it.nokkel } shouldNotContainAnyOf katalog.placeholdere.map { it.nokkel }
    }

    @Test
    fun `lovvalgsbegrepene er avgrenset til sakstyper med lovvalg`() {
        val betingelser = service.hentKatalog().betingelser

        betingelser.filter { it.sakstyper.isNotEmpty() }.map { it.nokkel } shouldContainExactly
            listOf("innvilgelse", "delvis-innvilgelse")
        betingelser.single { it.nokkel == "delvis-innvilgelse" }.sakstyper.map { it.name } shouldContainExactly
            listOf("EU_EOS", "TRYGDEAVTALE")
    }

    @Test
    fun `oppfylte fakta gir oppfylt betingelse`() {
        medSakskontekst(sakskontekst(fakta = fakta(oppfylt = true)))

        betingelser().values.forEach { it shouldBe true }
        betingelser().keys.toList() shouldContainExactly service.hentKatalog().betingelser.map { it.nokkel }
    }

    @Test
    fun `usanne fakta gir betingelser som ikke er oppfylt`() {
        medSakskontekst(sakskontekst(fakta = fakta(oppfylt = false)))

        betingelser().values.forEach { it shouldBe false }
    }

    @Test
    fun `utilgjengelige fakta utelates fra betingelsene`() {
        medSakskontekst(sakskontekst(fakta = BetingelseFakta()))

        service.hentVerdier(BEHANDLING_ID).betingelser.shouldBeEmpty()
    }

    @Test
    fun `bare de faktaene som er tilgjengelige svares ut`() {
        medSakskontekst(sakskontekst(fakta = BetingelseFakta(erAvslag = true, erUtsending = false)))

        service.hentVerdier(BEHANDLING_ID).betingelser shouldContainExactly listOf(
            BetingelseVerdi("avslag", true),
            BetingelseVerdi("utsending", false),
        )
    }

    @Test
    fun `betingelse som kaster utelates og logges sammen med de ovrige utelatte noklene`() {
        val register = register(
            definisjon(),
            betingelser = listOf(
                betingelse(nokkel = "kaster", vurdering = { error("Vurderingen feilet") }),
                betingelse(nokkel = "oppfylt", vurdering = { true }),
            ),
        )

        withLogAppender<PlaceholderService> { appender ->
            service(register).hentVerdier(BEHANDLING_ID).betingelser shouldContainExactly
                listOf(BetingelseVerdi("oppfylt", true))

            appender.list.single().formattedMessage shouldContain "kaster"
        }
    }

    // Uten filteret ville en FTRL-sak fatt «innvilgelse: false», som er noe annet enn «gjelder ikke her»
    @Test
    fun `betingelser avgrenset til lovvalgssaker utelates for andre sakstyper`() {
        medSakskontekst(sakskontekst(sakstype = Sakstyper.FTRL, fakta = fakta(oppfylt = true)))

        val nokler = betingelser().keys
        nokler shouldNotContainAnyOf listOf("innvilgelse", "delvis-innvilgelse")
        nokler shouldContainAll listOf("avslag", "opphort", "utsending")
    }

    @Test
    fun `lovvalgssak far ogsaa de avgrensede betingelsene`() {
        medSakskontekst(sakskontekst(sakstype = Sakstyper.EU_EOS, fakta = fakta(oppfylt = true)))

        betingelser().keys shouldContainAll listOf("innvilgelse", "delvis-innvilgelse")
    }

    private fun verdier(): Map<String, PlaceholderVerdi> = service.hentVerdier(BEHANDLING_ID).verdier.associateBy { it.nokkel }

    private fun betingelser(): Map<String, Boolean> =
        service.hentVerdier(BEHANDLING_ID).betingelser.associate { it.nokkel to it.oppfylt }

    private fun fakta(oppfylt: Boolean) = BetingelseFakta(
        erInnvilgelse = oppfylt,
        erAvslag = oppfylt,
        erOpphørt = oppfylt,
        erDelvisInnvilgelse = oppfylt,
        harÅpenSluttdato = oppfylt,
        erSkattepliktig = oppfylt,
        harLønnFraNorge = oppfylt,
        harInntektFraUtlandet = oppfylt,
        trygdeavgiftTilSkatt = oppfylt,
        erUtsending = oppfylt,
        erPensjonist = oppfylt,
        erFørstegangsvurdering = oppfylt,
        erNyVurdering = oppfylt,
    )

    private fun betingelse(nokkel: String, vurdering: (PlaceholderKontekst) -> Boolean?) = BetingelseDefinisjon(
        nokkel = nokkel,
        visningsnavn = "Visningsnavn for $nokkel",
        beskrivelse = "Beskrivelse av $nokkel",
        vurdering = vurdering,
    )

    private fun service(register: PlaceholderRegister) = PlaceholderService(
        sakskontekstHenter,
        persondataFasade,
        organisasjonOppslagService,
        PlaceholderLandnavnOppslag(kodeverkService),
        register,
    )

    private fun medSakskontekst(sakskontekst: PlaceholderSakskontekst = sakskontekst()) {
        every { sakskontekstHenter.hent(BEHANDLING_ID) } returns sakskontekst
    }

    private fun sakskontekst(
        brukersAktørID: String? = FagsakTestFactory.BRUKER_AKTØR_ID,
        sakstype: Sakstyper? = null,
        erLovvalg: Boolean = false,
        lovvalgsperiode: PeriodeData? = null,
        medlemskapsperiodeFom: LocalDate? = null,
        medlemskapsperiodeTom: LocalDate? = null,
        avgiftspliktigPerioder: List<PeriodeData> = emptyList(),
        soknadsperioder: List<PeriodeData> = emptyList(),
        arbeidsland: List<String> = emptyList(),
        utenlandskeArbeidsgivere: List<String> = emptyList(),
        norskeArbeidsgivereOrgnumre: Set<String> = emptySet(),
        fakta: BetingelseFakta = BetingelseFakta(),
    ) = PlaceholderSakskontekst(
        saksnummer = "MEL-12345",
        brukersAktørID = brukersAktørID,
        sakstype = sakstype,
        erLovvalg = erLovvalg,
        lovvalgsperiode = lovvalgsperiode,
        medlemskapsperiodeFom = medlemskapsperiodeFom,
        medlemskapsperiodeTom = medlemskapsperiodeTom,
        avgiftspliktigPerioder = avgiftspliktigPerioder,
        soknadsperioder = soknadsperioder,
        arbeidsland = arbeidsland,
        utenlandskeArbeidsgivere = utenlandskeArbeidsgivere,
        norskeArbeidsgivereOrgnumre = norskeArbeidsgivereOrgnumre,
        fakta = fakta,
    )

    private fun register(
        vararg definisjoner: PlaceholderDefinisjon,
        betingelser: List<BetingelseDefinisjon> = emptyList(),
    ): PlaceholderRegister = mockk {
        every { this@mockk.definisjoner } returns definisjoner.toList()
        every { this@mockk.betingelser } returns betingelser
    }

    private fun definisjon(
        nokkel: String = "saksnummer",
        resolver: (PlaceholderKontekst) -> PlaceholderResultat? = { PlaceholderResultat(it.saksnummer) },
    ) = PlaceholderDefinisjon(
        nokkel = nokkel,
        visningsnavn = "Visningsnavn for $nokkel",
        beskrivelse = "Beskrivelse av $nokkel",
        eksempel = { "eksempel" },
        resolver = resolver,
    )

    private fun dato(dag: Int, måned: Int, år: Int): LocalDate = LocalDate.of(år, måned, dag)

    private fun innvilget(fom: LocalDate?, tom: LocalDate? = null) = PeriodeData(fom, tom, erInnvilget = true)

    private fun persondata(
        fødselsdatoVerdi: LocalDate? = LocalDate.of(2024, 3, 15),
        bostedsadresse: Bostedsadresse? = bostedsadresse(),
        oppholdsadresser: Collection<Oppholdsadresse> = emptyList(),
        kontaktadresser: Collection<Kontaktadresse> = emptyList(),
    ): Persondata = Personopplysninger(
        emptyList(),
        bostedsadresse,
        null,
        emptySet(),
        Foedsel(fødselsdatoVerdi, null, null, null),
        Folkeregisteridentifikator("12345678901"),
        KjoennType.UKJENT,
        kontaktadresser,
        Navn("Ola", null, "Nordmann"),
        oppholdsadresser,
        emptySet(),
    )

    private fun strukturertAdresse(
        gatenavn: String = "Storgata",
        landkode: String = "NO",
        postnummer: String? = "0155",
    ) = StrukturertAdresse(
        gatenavn = gatenavn,
        husnummerEtasjeLeilighet = "1",
        postnummer = postnummer,
        poststed = "Oslo",
        landkode = landkode,
    )

    private fun bostedsadresse(landkode: String = "NO", coAdressenavn: String? = null) =
        Bostedsadresse(strukturertAdresse(landkode = landkode), coAdressenavn, null, null, Master.PDL.name, null, false)

    private fun oppholdsadresse(
        gatenavn: String,
        registrertDato: LocalDateTime?,
        erHistorisk: Boolean = false,
        postnummer: String? = "0155",
    ) = Oppholdsadresse(
        strukturertAdresse(gatenavn, postnummer = postnummer),
        null, null, null, Master.PDL.name, null, registrertDato, erHistorisk,
    )

    private fun kontaktadresse(
        registrertDato: LocalDateTime?,
        strukturertAdresse: StrukturertAdresse? = null,
        semistrukturertAdresse: SemistrukturertAdresse? = null,
    ) = Kontaktadresse(
        strukturertAdresse, semistrukturertAdresse, null, null, null, Master.PDL.name, null, registrertDato, false,
    )

    /** Godtar også gårsdagen, slik at et døgnskifte under testkjøringen ikke gjør testen flaky. */
    private fun dagensDatoAlternativer(): Set<String> =
        setOf(LocalDate.now(), LocalDate.now().minusDays(1))
            .map { it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
            .toSet()

    private companion object {
        const val BEHANDLING_ID = 1234L
    }
}
