package no.nav.melosys.service.persondata

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.Personstatus
import no.nav.melosys.domain.kodeverk.Personstatuser
import no.nav.melosys.domain.person.*
import no.nav.melosys.domain.person.familie.Familiemedlem
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.pdl.PDLConsumer
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident
import no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.*
import no.nav.melosys.integrasjon.pdl.dto.identer.Identliste
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse
import no.nav.melosys.integrasjon.pdl.dto.person.AdressebeskyttelseGradering
import no.nav.melosys.service.SaksbehandlingDataFactory.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PdlObjectFactory.*
import no.nav.melosys.service.persondata.familie.FamiliemedlemService
import no.nav.melosys.service.persondata.mapping.FamiliemedlemOversetter
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*
import java.util.function.Predicate

@ExtendWith(MockKExtension::class)
class PersondataServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @MockK
    private lateinit var pdlConsumer: PDLConsumer

    @MockK
    private lateinit var saksopplysningerService: SaksopplysningerService

    @MockK
    private lateinit var familiemedlemService: FamiliemedlemService

    private lateinit var persondataService: PersondataService

    @BeforeEach
    fun setup() {
        persondataService = PersondataService(
            behandlingService,
            kodeverkService,
            pdlConsumer,
            saksopplysningerService,
            familiemedlemService
        )
    }

    @Test
    fun `hentAktørID finnes verifiser aktørId`() {
        every { pdlConsumer.hentIdenter(any()) } returns lagIdentliste()


        val result = persondataService.hentAktørIdForIdent("123")


        result shouldBe "11111"
    }

    @Test
    fun `hentAktørID finnes ikke feiler`() {
        every { pdlConsumer.hentIdenter("321") } returns lagTomIdentliste()


        val exception = shouldThrow<IkkeFunnetException> {
            persondataService.hentAktørIdForIdent("321")
        }


        exception.message shouldContain "Finner ikke aktørID"
    }

    @Test
    fun `hentFolkeregisterIdent finnes verifiser ident`() {
        every { pdlConsumer.hentIdenter(any()) } returns lagIdentliste()


        val result = persondataService.hentFolkeregisterident("123")


        result shouldBe "22222"
    }

    @Test
    fun `hentFolkeregisterIdent finnes ikke feiler`() {
        every { pdlConsumer.hentIdenter(any()) } returns lagTomIdentliste()


        val exception = shouldThrow<IkkeFunnetException> {
            persondataService.hentFolkeregisterident("123")
        }


        exception.message shouldContain "Finner ikke folkeregisterident"
    }

    @Test
    fun `hentPersonMedFamilie returnerer person med familie`() {
        val forventetRelatertVedSivilstandID = "forventetRelatertVedSivilstandID"
        every { pdlConsumer.hentPerson(any()) } returns lagPerson()
        every { familiemedlemService.hentFamiliemedlemmer(lagPerson()) } returns setOf(
            FamiliemedlemOversetter.oversettBarn(lagPerson(), lagFolkeregisterIdent("identForelder1")),
            FamiliemedlemOversetter.oversettEktefelleEllerPartner(
                lagPerson(),
                lagSivilstand(forventetRelatertVedSivilstandID)
            )
        )
        every { kodeverkService.dekod(any(), any()) } returns "Mocked value"


        val persondata = persondataService.hentPerson(
            "IdNr",
            Informasjonsbehov.MED_FAMILIERELASJONER
        ) as Personopplysninger


        persondata.run {
            bostedsadresse.shouldNotBeNull()
            dødsfall shouldBe Doedsfall(LocalDate.MAX)
            fødsel shouldBe Foedsel(LocalDate.parse("1970-01-01"), 1970, "NOR", "fødested")
            folkeregisteridentifikator shouldBe lagFolkeregisterIdent("IdNr")
            kjønn shouldBe KjoennType.UKJENT
            navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
            statsborgerskap shouldContainExactlyInAnyOrder listOf(
                Statsborgerskap(
                    "AIA", null, LocalDate.parse("1979-11-18"),
                    LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
                ),
                Statsborgerskap(
                    "NOR", LocalDate.parse("2021-05-08"), null,
                    null, "PDL", "Dolly", false
                )
            )

            familiemedlemmer.shouldNotBeNull().shouldHaveSize(2).toList().run {
                get(0).run {
                    erRelatertVedSivilstand() shouldBe false
                    erBarn() shouldBe true
                    harForventetRelatertVedSivilstandId(forventetRelatertVedSivilstandID).test(this) shouldBe false
                }
                get(1).run {
                    erRelatertVedSivilstand() shouldBe true
                    erBarn() shouldBe false
                    harForventetRelatertVedSivilstandId(forventetRelatertVedSivilstandID).test(this) shouldBe true
                }
            }
        }
    }

    @Test
    fun `hentPersonMedHistorikk aktiv behandling konvertering ok`() {
        every { behandlingService.hentBehandling(1L) } returns lagBehandling()
        every { pdlConsumer.hentPersonMedHistorikk(any()) } returns lagPerson()
        every { kodeverkService.dekod(any(), any()) } returns "Mocked value"


        val personMedHistorikk = persondataService.hentPersonMedHistorikk(1L)


        personMedHistorikk.run {
            bostedsadresser.shouldNotBeEmpty()
            dødsfall shouldBe Doedsfall(LocalDate.MAX)
            fødsel shouldBe Foedsel(LocalDate.parse("1970-01-01"), 1970, "NOR", "fødested")
            folkeregisteridentifikator shouldBe lagFolkeregisterIdent("IdNr")
            folkeregisterpersonstatuser.map { it.personstatus } shouldContainExactly listOf(Personstatuser.IKKE_BOSATT)
            kjønn shouldBe KjoennType.UKJENT
            navn shouldBe Navn("fornavn", "mellomnavn", "etternavn")
            statsborgerskap shouldContainExactlyInAnyOrder listOf(
                Statsborgerskap(
                    "AIA", null, LocalDate.parse("1979-11-18"),
                    LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
                ),
                Statsborgerskap(
                    "NOR", LocalDate.parse("2021-05-08"), null,
                    null, "PDL", "Dolly", false
                )
            )
        }
    }

    @Test
    fun `hentPersonMedHistorikk inaktiv behandling inaktiv behandling fra før PDL`() {
        val inaktivBehandling = lagInaktivBehandlingSomIkkeResulterIVedtak()
        val sivilstand = mockk<no.nav.melosys.domain.dokument.person.Sivilstand>(relaxed = true)
        every { behandlingService.hentBehandling(1L) } returns inaktivBehandling
        every { saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L) } returns Optional.empty()
        every { sivilstand.kode } returns "GLAD"
        every { saksopplysningerService.hentTpsPersonopplysninger(inaktivBehandling.id) } returns lagPersonDokument(sivilstand)
        every { kodeverkService.dekod(any(), any()) } returns "Mocked value"


        val personMedHistorikk = persondataService.hentPersonMedHistorikk(1L)


        personMedHistorikk.statsborgerskap shouldContainExactly listOf(
            Statsborgerskap(
                "NOR", null, LocalDate.parse("1989-08-07"),
                null, "TPS", "TPS", false
            )
        )
    }

    @Test
    fun `hentPersonMedHistorikk inaktiv behandling returnerer data fra PDL`() {
        every { behandlingService.hentBehandling(1L) } returns lagInaktivBehandling()
        every { saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L) } returns
            Optional.of(PersonopplysningerObjectFactory.lagPersonMedHistorikk())


        val personMedHistorikk = persondataService.hentPersonMedHistorikk(1L)


        personMedHistorikk.statsborgerskap shouldContainExactlyInAnyOrder listOf(
            Statsborgerskap(
                "AAA", null, LocalDate.parse("2009-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
            ),
            Statsborgerskap(
                "BBB", null, LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
            ),
            Statsborgerskap(
                "CCC", null, null,
                LocalDate.parse("1980-11-18"), "PDL", "Dolly", false
            )
        )
    }

    @Test
    fun `hentPersonMedHistorikk inaktiv behandling TPS data lagret returnerer data fra TPS`() {
        every { behandlingService.hentBehandling(1L) } returns lagInaktivBehandling()
        every { saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L) } returns Optional.empty()
        every { saksopplysningerService.hentTpsPersonopplysninger(1L) } returns lagPersonDokument(null)
        every { kodeverkService.dekod(any(), any()) } returns "Mocked value"


        val personMedHistorikk = persondataService.hentPersonMedHistorikk(1L)


        personMedHistorikk.statsborgerskap shouldContainExactly listOf(
            Statsborgerskap(
                "NOR", null, LocalDate.parse("1989-08-07"),
                null, "TPS", "TPS", false
            )
        )
    }

    @Test
    fun `hentSammensatNavn returnerer formatert navn`() {
        every { pdlConsumer.hentNavn(any()) } returns setOf(
            no.nav.melosys.integrasjon.pdl.dto.person.Navn(
                "Fornavn", "Mellom", "Etternavnsen", metadata()
            )
        )


        val result = persondataService.hentSammensattNavn("")


        result shouldBe "Etternavnsen Mellom Fornavn"
    }

    @Test
    fun `hentStatsborgerskap returnerer statsborgerskap`() {
        every { pdlConsumer.hentStatsborgerskap("ident") } returns setOf(
            no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap(
                "AIA",
                LocalDate.parse("2021-05-08"),
                LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"),
                metadata()
            )
        )


        val result = persondataService.hentStatsborgerskap("ident")


        result shouldContainExactly listOf(
            Statsborgerskap(
                "AIA",
                LocalDate.parse("2021-05-08"),
                LocalDate.parse("1979-11-18"),
                LocalDate.parse("1980-11-18"),
                "PDL",
                "Dolly",
                false
            )
        )
    }

    @Test
    fun `harStrengtFortroligAdresse returnerer true når strengt fortrolig finnes`() {
        every { pdlConsumer.hentAdressebeskyttelser(any()) } returns listOf(
            Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT, metadata()),
            Adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG, metadata())
        )


        val result = persondataService.harStrengtFortroligAdresse("")


        result.shouldBeTrue()
    }

    private fun harForventetRelatertVedSivilstandId(forventetRelatertVedSivilstandID: String): Predicate<Familiemedlem> =
        Predicate { familiemedlem ->
            familiemedlem.sivilstand() != null &&
                forventetRelatertVedSivilstandID == familiemedlem.sivilstand().relatertVedSivilstand()
        }

    private fun lagFolkeregisterIdent(identForelder1: String): Folkeregisteridentifikator =
        Folkeregisteridentifikator(identForelder1)

    private fun lagPersonDokument(sivilstand: no.nav.melosys.domain.dokument.person.Sivilstand?): PersonDokument =
        PersonDokument().apply {
            kjønn = KjoennsType("K")
            fornavn = "Kari"
            mellomnavn = "Mellom"
            etternavn = "Nordmann"
            fødselsdato = LocalDate.parse("1989-08-07")
            fnr = "123456789"
            bostedsadresse = BrevDataTestUtils.lagBostedsadresse()
            postadresse = DokgenTestData.lagAdresse()
            personstatus = Personstatus.ABNR
            this.sivilstand = sivilstand
            sivilstandGyldighetsperiodeFom = LocalDate.parse("2019-08-07")
            statsborgerskap = Land(Land.NORGE)
            statsborgerskapDato = LocalDate.parse("1989-08-07")
            dødsdato = LocalDate.parse("2089-08-07")
        }

    private fun lagIdentliste(): Identliste {
        val identliste = Identliste(hashSetOf())
        identliste.identer().add(Ident("11111", AKTORID))
        identliste.identer().add(Ident("22222", FOLKEREGISTERIDENT))
        identliste.identer().add(Ident("33333", NPID))
        return identliste
    }

    private fun lagTomIdentliste(): Identliste = Identliste(emptySet())
}
