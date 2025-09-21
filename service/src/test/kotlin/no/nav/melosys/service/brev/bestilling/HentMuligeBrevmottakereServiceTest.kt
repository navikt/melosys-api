package no.nav.melosys.service.brev.bestilling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.brev.Mottakerliste
import no.nav.melosys.domain.brev.NorskMyndighet.SKATTEETATEN
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Mottakerroller.*
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.brev.DokumentNavnService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class HentMuligeBrevmottakereServiceTest {

    private val behandling = lagBehandling()

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var brevmottakerService: BrevmottakerService

    @RelaxedMockK
    lateinit var dokumentNavnService: DokumentNavnService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var eregFasade: EregFasade

    @RelaxedMockK
    lateinit var kontaktopplysningService: KontaktopplysningService

    @RelaxedMockK
    lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    private lateinit var hentMuligeBrevmottakere: HentMuligeBrevmottakereService

    @BeforeEach
    fun init() {
        // Mock kontaktopplysningService to return empty Optional by default
        every { kontaktopplysningService.hentKontaktopplysning(any(), any()) } returns Optional.empty()

        hentMuligeBrevmottakere = HentMuligeBrevmottakereService(
            behandlingService, brevmottakerService, dokumentNavnService, persondataFasade,
            eregFasade, kontaktopplysningService, utenlandskMyndighetService
        )
    }

    @Test
    fun `hentMuligeMottakere skal returnere bruker som hovedmottaker når hovedmottaker er bruker`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123L) } returns Mottakerliste(BRUKER)
        every { brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)) } returns lagMottakerPerson(BRUKER, null)
        every { persondataFasade.hentSammensattNavn(any()) } returns "Ola Nordmann"
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
            mottakerNavn shouldBe "Ola Nordmann"
            rolle shouldBe BRUKER
            aktørId shouldBe null
            orgnr shouldBe null
        }

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun `hentMuligeMottakere skal returnere fullmektig organisasjon som hovedmottaker når bruker har fullmektig organisasjon`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(BRUKER)
        every { brevmottakerService.avklarMottaker(any(), any(), eq(behandling)) } returns lagMottakerOrg(FULLMEKTIG, "orgnr")
        mockHentOrganisasjon("orgnr", "Fullmektig virksomhet")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
            mottakerNavn shouldBe "Fullmektig virksomhet"
            rolle shouldBe BRUKER
            aktørId shouldBe null
            orgnr shouldBe null
        }

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun `hentMuligeMottakere skal returnere fullmektig person som hovedmottaker når bruker har fullmektig person`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(BRUKER)
        every { brevmottakerService.avklarMottaker(any(), any(), eq(behandling)) } returns lagMottakerPerson(FULLMEKTIG, "fnr")
        every { persondataFasade.hentSammensattNavn("fnr") } returns "Ola Nordmann"
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
            mottakerNavn shouldBe "Ola Nordmann"
            rolle shouldBe BRUKER
            aktørId shouldBe null
            orgnr shouldBe null
        }

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun `hentMuligeMottakere skal returnere virksomhet som hovedmottaker når hovedmottaker er virksomhet`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L) } returns Mottakerliste(VIRKSOMHET)
        mockFinnOrganisasjon("orgnr", "Equinor AS")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                GENERELT_FRITEKSTBREV_VIRKSOMHET,
                VIRKSOMHET
            )
        } returns GENERELT_FRITEKSTBREV_VIRKSOMHET.beskrivelse

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(GENERELT_FRITEKSTBREV_VIRKSOMHET, 123L, "orgnr", null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe GENERELT_FRITEKSTBREV_VIRKSOMHET.beskrivelse
            mottakerNavn shouldBe "Equinor AS"
            rolle shouldBe VIRKSOMHET
            aktørId shouldBe null
            orgnr shouldBe null
        }

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun `hentMuligeMottakere skal returnere arbeidsgiver som hovedmottaker når hovedmottaker er arbeidsgiver`() {
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(ARBEIDSGIVER)
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                any(),
                MANGELBREV_BRUKER,
                ARBEIDSGIVER
            )
        } returns MANGELBREV_BRUKER.beskrivelse

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, "orgnr", null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
            mottakerNavn shouldBe "Ola Nordmann Rørleggerfirma"
            rolle shouldBe ARBEIDSGIVER
            aktørId shouldBe null
            orgnr shouldBe null
        }

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun `hentMuligeMottakere skal returnere bruker som kopi når kopi til bruker`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            ARBEIDSGIVER,
            emptyList(),
            listOf(BRUKER),
            emptyList()
        )
        every { persondataFasade.hentSammensattNavn(any()) } returns "Ola Nordmann"
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma")
        every { brevmottakerService.avklarMottaker(any(), any(), eq(behandling)) } returns lagMottakerOrg(BRUKER, null)

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, "orgnr", null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 1
        kopiMottakere.first().run {
            dokumentNavn shouldBe "Kopi til bruker"
            mottakerNavn shouldBe "Ola Nordmann"
            rolle shouldBe BRUKER
            aktørId shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
            orgnr shouldBe null
        }
    }

    @Test
    fun `hentMuligeMottakere skal returnere fullmektig som kopi når bruker har fullmektig`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            ARBEIDSGIVER,
            emptyList(),
            listOf(BRUKER),
            emptyList()
        )
        every { brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)) } returns lagMottakerOrg(
            FULLMEKTIG,
            "orgnrTilFullmektig"
        )
        mockFinnOrganisasjon("orgnr", "Ola Nordmann Rørleggerfirma")
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet")

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, "orgnr", null)

        hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)
            .kopiMottakere()
            .shouldHaveSize(1)
            .single().run {
                dokumentNavn shouldBe "Kopi til brukers fullmektig"
                mottakerNavn shouldBe "Fullmektig Virksomhet"
                rolle shouldBe FULLMEKTIG
                aktørId shouldBe null
                orgnr shouldBe "orgnrTilFullmektig"
            }
    }

    @Test
    fun `hentMuligeMottakere skal returnere bruker som kopi når hovedmottaker er bruker med fullmektig`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            BRUKER,
            emptyList(),
            listOf(BRUKER),
            emptyList()
        )
        every { brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)) } returns lagMottakerOrg(
            FULLMEKTIG,
            "orgnrTilFullmektig"
        )
        every { persondataFasade.hentSammensattNavn(any()) } returns "Ola Nordmann"
        mockHentOrganisasjon("orgnrTilFullmektig", "Fullmektig Virksomhet")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
            mottakerNavn shouldBe "Fullmektig Virksomhet"
            rolle shouldBe BRUKER
            aktørId shouldBe null
            orgnr shouldBe null
        }

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 1
        kopiMottakere.first().run {
            dokumentNavn shouldBe "Kopi til bruker"
            mottakerNavn shouldBe "Ola Nordmann"
            rolle shouldBe BRUKER
            aktørId shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
            orgnr shouldBe null
        }
    }

    @Test
    fun `hentMuligeMottakere skal returnere arbeidsgiver som kopi når kopi til arbeidsgiver`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            BRUKER,
            emptyList(),
            listOf(ARBEIDSGIVER),
            emptyList()
        )
        every { brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)) } returns lagMottakerOrg(BRUKER, null)
        val orgnr1 = lagMottakerOrg(ARBEIDSGIVER, "orgnr1")
        val orgnr2 = lagMottakerOrg(ARBEIDSGIVER, "orgnr2")
        every { brevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), any(), any()) } returns listOf(orgnr1, orgnr2)
        mockHentOrganisasjon("orgnr1", "Arbeidsgiver 1")
        mockHentOrganisasjon("orgnr2", "Arbeidsgiver 2")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                MANGELBREV_BRUKER,
                orgnr1,
                "Kopi til arbeidsgiver"
            )
        } returns "Kopi til arbeidsgiver"
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                MANGELBREV_BRUKER,
                orgnr2,
                "Kopi til arbeidsgiver"
            )
        } returns "Kopi til arbeidsgiver"

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 2
        kopiMottakere[0].run {
            dokumentNavn shouldBe "Kopi til arbeidsgiver"
            mottakerNavn shouldBe "Arbeidsgiver 1"
            rolle shouldBe ARBEIDSGIVER
            aktørId shouldBe null
            orgnr shouldBe "orgnr1"
        }

        kopiMottakere[1].run {
            dokumentNavn shouldBe "Kopi til arbeidsgiver"
            mottakerNavn shouldBe "Arbeidsgiver 2"
            rolle shouldBe ARBEIDSGIVER
            aktørId shouldBe null
            orgnr shouldBe "orgnr2"
        }
    }

    @Test
    fun `hentMuligeMottakere skal returnere fullmektig som kopi når arbeidsgiver har fullmektig`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            BRUKER,
            emptyList(),
            listOf(ARBEIDSGIVER),
            emptyList()
        )
        every { brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)) } returns lagMottakerOrg(BRUKER, null)
        val fullmektigMottaker = lagMottakerOrg(FULLMEKTIG, "orgnr")
        every { brevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), any(), any()) } returns listOf(fullmektigMottaker)
        mockHentOrganisasjon("orgnr", "Fullmektig Virksomhet")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                MANGELBREV_BRUKER,
                fullmektigMottaker,
                "Kopi til arbeidsgivers fullmektig"
            )
        } returns "Kopi til arbeidsgivers fullmektig"

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 1
        kopiMottakere.first().run {
            dokumentNavn shouldBe "Kopi til arbeidsgivers fullmektig"
            mottakerNavn shouldBe "Fullmektig Virksomhet"
            rolle shouldBe FULLMEKTIG
            aktørId shouldBe null
            orgnr shouldBe "orgnr"
        }
    }

    @Test
    fun `hentMuligeMottakere skal returnere Skatteetaten som fast mottaker når fast til skatt`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            BRUKER,
            emptyList(),
            emptyList(),
            listOf(SKATTEETATEN)
        )
        every { brevmottakerService.avklarMottaker(MANGELBREV_BRUKER, Mottaker.medRolle(BRUKER), behandling) } returns lagMottakerOrg(BRUKER, null)
        val skatteetaten = Mottaker.av(SKATTEETATEN)
        every { brevmottakerService.avklarMottaker(MANGELBREV_BRUKER, Mottaker.av(SKATTEETATEN), behandling) } returns skatteetaten
        mockHentOrganisasjon("974761076", "Skatteetaten")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                MANGELBREV_BRUKER,
                BRUKER
            )
        } returns MANGELBREV_BRUKER.beskrivelse
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                MANGELBREV_BRUKER,
                skatteetaten,
                "Kopi til Skatteetaten"
            )
        } returns "Kopi til Skatteetaten"

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        val fasteMottakere = muligeMottakere.fasteMottakere()
        fasteMottakere shouldHaveSize 1
        fasteMottakere.first().run {
            dokumentNavn shouldBe "Kopi til Skatteetaten"
            mottakerNavn shouldBe "Skatteetaten"
            rolle shouldBe NORSK_MYNDIGHET
            aktørId shouldBe null
            orgnr shouldBe "974761076"
        }
    }

    @Test
    fun `hentMuligeMottakere skal håndtere Storbritannia artikkel ulik 82 for hovedmottaker bruker`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(TRYGDEAVTALE_GB, 123L) } returns Mottakerliste(
            BRUKER,
            emptyList(),
            listOf(ARBEIDSGIVER, UTENLANDSK_TRYGDEMYNDIGHET),
            listOf(SKATTEETATEN)
        )
        every { brevmottakerService.avklarMottaker(eq(TRYGDEAVTALE_GB), any(), eq(behandling)) } returns lagMottakerOrg(BRUKER, null)
        val arbeidsgiver = lagMottakerOrg(ARBEIDSGIVER, "123")
        every { brevmottakerService.avklarMottakere(TRYGDEAVTALE_GB, Mottaker.medRolle(ARBEIDSGIVER), behandling, false, true) } returns listOf(
            arbeidsgiver
        )
        val trygdemyndighet = lagMottakerOrg(UTENLANDSK_TRYGDEMYNDIGHET, "456")
        every { brevmottakerService.avklarMottakere(TRYGDEAVTALE_GB, Mottaker.medRolle(UTENLANDSK_TRYGDEMYNDIGHET), behandling) } returns listOf(
            trygdemyndighet
        )
        val skatteetaten = lagMottakerOrg(UTENLANDSK_TRYGDEMYNDIGHET, "974761076")
        every { brevmottakerService.avklarMottaker(TRYGDEAVTALE_GB, Mottaker.av(SKATTEETATEN), behandling) } returns skatteetaten
        every { persondataFasade.hentSammensattNavn(any()) } returns "Ola Nordmann"
        mockHentOrganisasjon("123", "Ståle Stål")
        mockHentOrganisasjon("974761076", "Skatt")
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                TRYGDEAVTALE_GB,
                BRUKER
            )
        } returns "Vedtak om medlemskap, Attest for medlemskap i folketrygden"
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                TRYGDEAVTALE_GB,
                arbeidsgiver,
                "Kopi til arbeidsgiver"
            )
        } returns "Kopi av vedtak om medlemskap, Attest for medlemskap i folketrygden"
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                TRYGDEAVTALE_GB,
                trygdemyndighet,
                "Kopi til utenlandsk trygdemyndighet"
            )
        } returns "Attest for medlemskap i folketrygden"
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottaker(
                behandling,
                TRYGDEAVTALE_GB,
                skatteetaten,
                "Kopi til Skatt"
            )
        } returns "Kopi av vedtak om medlemskap"

        val request = HentMuligeBrevmottakereService.RequestDto(TRYGDEAVTALE_GB, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(request)

        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe "Vedtak om medlemskap, Attest for medlemskap i folketrygden"
            mottakerNavn shouldBe "Ola Nordmann"
            rolle shouldBe BRUKER
            aktørId shouldBe null
            orgnr shouldBe null
        }

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 2

        val fasteMottakere = muligeMottakere.fasteMottakere()
        fasteMottakere shouldHaveSize 1
        fasteMottakere.first().run {
            dokumentNavn shouldBe "Kopi av vedtak om medlemskap"
            mottakerNavn shouldBe "Skatt"
        }
    }

    @Test
    fun `hentMuligeMottakere skal hente navn fra oppgitt institusjon ID når hovedmottaker er utenlandsk trygdemyndighet`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV, 123L) } returns Mottakerliste(
            UTENLANDSK_TRYGDEMYNDIGHET
        )
        every {
            dokumentNavnService.utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
                behandling,
                UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
                UTENLANDSK_TRYGDEMYNDIGHET
            )
        } returns "Fritekstbrev"
        val utenlandskMyndighetGB = UtenlandskMyndighet().apply {
            landkode = Land_iso2.GB
            navn = "PT Operations"
            postnummer = "123"
        }
        every { utenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID("GB:UK010") } returns utenlandskMyndighetGB

        val request = HentMuligeBrevmottakereService.RequestDto(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV, 123L, null, "GB:UK010")

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(request)

        verify(exactly = 0) { brevmottakerService.avklarMottaker(any(), any(), any()) }
        muligeMottakere.hovedMottaker().run {
            dokumentNavn shouldBe "Fritekstbrev"
            mottakerNavn shouldBe "PT Operations"
            rolle shouldBe UTENLANDSK_TRYGDEMYNDIGHET
            aktørId shouldBe null
            orgnr shouldBe null
        }
    }

    private fun lagMottakerOrg(rolle: Mottakerroller, orgNummer: String?): Mottaker {
        val mottaker = Mottaker.medRolle(rolle)
        mottaker.orgnr = orgNummer
        return mottaker
    }

    private fun lagMottakerPerson(rolle: Mottakerroller, personIdent: String?): Mottaker {
        val mottaker = Mottaker.medRolle(rolle)
        mottaker.personIdent = personIdent
        return mottaker
    }

    private fun lagBehandling() = Behandling.forTest {
        fagsak {
            medBruker()
        }
    }.apply {
        saksopplysninger.add(lagPERSOPLSaksopplysning())
    }

    private fun mockHentOrganisasjon(orgnr: String, navn: String) {
        every { eregFasade.hentOrganisasjon(orgnr) } returns lagOrgSaksopplysning(orgnr, navn)
    }

    private fun mockFinnOrganisasjon(orgnr: String, navn: String) {
        every { eregFasade.finnOrganisasjon(orgnr) } returns Optional.of(lagOrgSaksopplysning(orgnr, navn))
    }

    private fun lagOrgSaksopplysning(orgNummer: String, navn: String): Saksopplysning {
        val geogragiskAdresse = SemistrukturertAdresse().apply {
            adresselinje1 = "Gateadresse 43A"
            postnr = "0123"
            poststed = "Oslo"
            landkode = Land.NORGE
            gyldighetsperiode = Periode(LocalDate.now().minusYears(10), null)
        }
        val organisasjonsDetaljer = OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(geogragiskAdresse)
            .build()
        val dokument = OrganisasjonDokumentTestFactory.builder()
            .orgnummer(orgNummer)
            .navn(navn)
            .organisasjonsDetaljer(organisasjonsDetaljer)
            .build()
        return Saksopplysning().apply {
            this.dokument = dokument
            type = SaksopplysningType.ORG
        }
    }

    private fun lagPERSOPLSaksopplysning() = Saksopplysning().apply {
        this.dokument = PersonDokument().apply {
            fnr = "12345678910"
            sammensattNavn = "Ola Nordmann"
            gjeldendePostadresse.adresselinje1 = "Gateadresse 43A"
            gjeldendePostadresse.postnr = "0123"
            gjeldendePostadresse.land = Land.av(Land.NORGE)
        }
        type = SaksopplysningType.PERSOPL
    }
}
