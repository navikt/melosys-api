package no.nav.melosys.service.brev.bestilling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.brev.BrevAdresse
import no.nav.melosys.service.brev.TilBrevAdresseService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class TilBrevAdresseServiceTest {

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var kontaktopplysningService: KontaktopplysningService

    @MockK
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    @MockK
    private lateinit var eregFasade: EregFasade

    @InjectMockKs
    private lateinit var tilBrevAdresseService: TilBrevAdresseService

    private val behandling = lagBehandling()

    @Test
    fun `tilBrevAdresse brukerSomMottaker returnererBrukeradresse`() {
        every { persondataFasade.hentPerson(any()) } returns lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER)


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser shouldBe BrevAdresse(
            mottakerNavn = "Nordmann Ola",
            orgnr = null,
            adresselinjer = listOf("gatenavnFraBostedsadresse 3"),
            postnr = "1234",
            poststed = "Oslo",
            region = "Norge",
            land = "NO"
        )
    }

    @Test
    fun `tilBrevAdresse brukersFullmaktOrganisasjonSomMottaker returnererFullmektigsAdresse`() {
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns java.util.Optional.empty()
        every { eregFasade.hentOrganisasjon("orgnr") } returns lagOrgSaksopplysning("orgnr", "Ola Nordmann Fullmektig")
        val mottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG).apply {
            orgnr = "orgnr"
        }


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser shouldBe BrevAdresse(
            mottakerNavn = "Ola Nordmann Fullmektig",
            orgnr = "orgnr",
            adresselinjer = listOf("Gateadresse 43A"),
            postnr = "0123",
            poststed = "Oslo",
            region = null,
            land = Land.NORGE
        )
    }

    @Test
    fun `tilBrevAdresse brukersFullmaktPersonSomMottaker returnererFullmektigsAdresse`() {
        every { persondataFasade.hentPerson("fnr") } returns lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse()
        val mottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG).apply {
            personIdent = "fnr"
        }


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser shouldBe BrevAdresse(
            mottakerNavn = "Nordmann Ola",
            orgnr = null,
            adresselinjer = listOf("gatenavnFraBostedsadresse 3"),
            postnr = "1234",
            poststed = "Oslo",
            region = "Norge",
            land = "NO"
        )
    }

    @Test
    fun `tilBrevAdresse arbeidsgiverSomMottaker returnererArbeidsgiverAdresser`() {
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns java.util.Optional.empty()
        every { eregFasade.hentOrganisasjon("orgnr") } returns lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma")
        val mottaker = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER).apply {
            orgnr = "orgnr"
        }


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser shouldBe BrevAdresse(
            mottakerNavn = "Ola Nordmann Rørleggerfirma",
            orgnr = "orgnr",
            adresselinjer = listOf("Gateadresse 43A"),
            postnr = "0123",
            poststed = "Oslo",
            region = null,
            land = Land.NORGE
        )
    }

    @Test
    fun `tilBrevAdresse virksomhetSomMottaker returnererVirksomhetAdresser`() {
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns java.util.Optional.empty()
        every { eregFasade.hentOrganisasjon("orgnr") } returns lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma")
        val mottaker = Mottaker.medRolle(Mottakerroller.VIRKSOMHET).apply {
            orgnr = "orgnr"
        }


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser shouldBe BrevAdresse(
            mottakerNavn = "Ola Nordmann Rørleggerfirma",
            orgnr = "orgnr",
            adresselinjer = listOf("Gateadresse 43A"),
            postnr = "0123",
            poststed = "Oslo",
            region = null,
            land = Land.NORGE
        )
    }

    @Test
    fun `tilBrevAdresse norskMyndighetSomMottaker kasterFeil`() {
        val mottaker = Mottaker.medRolle(Mottakerroller.NORSK_MYNDIGHET)


        val exception = shouldThrow<FunksjonellException> {
            tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)
        }


        exception.message shouldContain "Mottakersrolle støttes ikke: NORSK_MYNDIGHET"
    }

    @Test
    fun `tilBrevAdresse utenlandsakTrygdemyndighetSomMottaker kasterFeil`() {
        val mottaker = Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)


        val exception = shouldThrow<FunksjonellException> {
            tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)
        }


        exception.message shouldContain "Mottakersrolle støttes ikke: UTENLANDSK_TRYGDEMYNDIGHET"
    }

    @Test
    fun `tilBrevAdresse returnererAdresseFelterSomNull nårGjeldendePostadresseErNull`() {
        val persondata = lagPersonopplysningerUtenAdresser()
        every { persondataFasade.hentPerson(any()) } returns persondata
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER).apply {
            orgnr = null
        }


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser shouldBe BrevAdresse(
            mottakerNavn = "Nordmann Ola",
            orgnr = null,
            adresselinjer = null,
            postnr = null,
            poststed = null,
            region = null,
            land = null
        )
    }

    @Test
    fun `hentBrevAdresseTilMottakere returnererAdresseMedKorrektAdresselinjer nårCoAdresseErTomStreng`() {
        every { persondataFasade.hentPerson(any()) } returns lagPersondataMedTomCo()
        val mottaker = Mottaker.medRolle(Mottakerroller.BRUKER).apply {
            orgnr = null
        }


        val brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling)


        brevAdresser.adresselinjer shouldBe listOf("gatenavnFraBostedsadresse 3")
    }

    @Test
    fun `tilBrevAdresse verkenPersonIdentEllerOrgnr kasterFeil`() {
        val exception = shouldThrow<FunksjonellException> {
            tilBrevAdresseService.tilBrevAdresse(null as String?, null)
        }
        exception.message shouldContain "Kan ikke finne adresse uten personIdent og organisasjonsnummer"
    }

    @Test
    fun `tilBrevAdresse finnerIkkePersonDataFraPersonIdent kasterFeil`() {
        every { persondataFasade.hentPerson("123") } returns null

        val exception = shouldThrow<FunksjonellException> {
            tilBrevAdresseService.tilBrevAdresse("123", null)
        }
        exception.message shouldContain "Finner ikke persondata for personIdent"
    }

    @Test
    fun `tilBrevAdresse personIdentSendesInn returnererPersonAdresse`() {
        every { persondataFasade.hentPerson("123") } returns lagPersonopplysninger()


        val brevAdresse = tilBrevAdresseService.tilBrevAdresse("123", null)


        verify {
            persondataFasade.hentPerson("123")
        }
        brevAdresse shouldBe BrevAdresse(
            mottakerNavn = "Nordmann Ola",
            orgnr = null,
            adresselinjer = listOf("gatenavnKontaktadressePDL"),
            postnr = "0123",
            poststed = "Poststed",
            region = null,
            land = "NO"
        )
    }

    @Test
    fun `tilBrevAdresse orgnrSendesInn returnererOrganisasjonsAdresse`() {
        every { eregFasade.hentOrganisasjon("orgnr") } returns lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma")


        val brevAdresse = tilBrevAdresseService.tilBrevAdresse(null, "orgnr")


        verify {
            eregFasade.hentOrganisasjon("orgnr")
        }
        brevAdresse shouldBe BrevAdresse(
            mottakerNavn = "Ola Nordmann Rørleggerfirma",
            orgnr = "orgnr",
            adresselinjer = listOf("Gateadresse 43A"),
            postnr = "0123",
            poststed = "Oslo",
            region = null,
            land = Land.NORGE
        )
    }

    private fun lagBehandling(): Behandling {
        val behandling = Behandling.forTest {
            fagsak {
                medBruker()
            }
        }
        behandling.saksopplysninger.add(lagPERSOPLSaksopplysning())
        return behandling
    }

    private fun lagPERSOPLSaksopplysning() = Saksopplysning().apply {
        dokument = PersonDokument().apply {
            fnr = "12345678910"
            sammensattNavn = "Ola Nordmann"
            gjeldendePostadresse.apply {
                adresselinje1 = "Gateadresse 43A"
                postnr = "0123"
                land = Land.av(Land.NORGE)
            }
        }
        type = SaksopplysningType.PERSOPL
    }

    private fun lagOrgSaksopplysning(orgNummer: String, navn: String) = Saksopplysning().apply {
        val geogragiskAdresse = SemistrukturertAdresse().apply {
            adresselinje1 = "Gateadresse 43A"
            postnr = "0123"
            poststed = "Oslo"
            landkode = Land.NORGE
            gyldighetsperiode = Periode(LocalDate.MIN, LocalDate.MAX)
        }

        val organisasjonsDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(geogragiskAdresse)
            .build()
        organisasjonsDetaljer.postadresse = listOf(geogragiskAdresse)

        dokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer(orgNummer)
            .navn(navn)
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()
        type = SaksopplysningType.ORG
    }

    private fun lagPersondataMedTomCo(): Persondata = Personopplysninger(
        emptyList(),
        Bostedsadresse(
            StrukturertAdresse("gatenavnFraBostedsadresse 3", null, null, null, null, null),
            "", null, null, null, null, false
        ),
        null, emptySet(), null, null,
        null, emptySet(), Navn(null, null, null), emptySet(), emptySet()
    )
}
