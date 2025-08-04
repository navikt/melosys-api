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
class HentMuligeBrevmottakereServiceKtTest {

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
    fun hentMuligeMottakere_hovedMottakerBruker_returnererBrukerSomHovedMottaker() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
        hovedMottaker.mottakerNavn shouldBe "Ola Nordmann"
        hovedMottaker.rolle shouldBe BRUKER
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun hentMuligeMottakere_hovedMottakerBrukerMedFullmektigOrganisasjon_returnererFullmektigOrganisasjonSomHovedMottaker() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
        hovedMottaker.mottakerNavn shouldBe "Fullmektig virksomhet"
        hovedMottaker.rolle shouldBe BRUKER
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun hentMuligeMottakere_hovedMottakerBrukerMedFullmektigPerson_returnererFullmektigPersonSomHovedMottaker() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
        hovedMottaker.mottakerNavn shouldBe "Ola Nordmann"
        hovedMottaker.rolle shouldBe BRUKER
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun hentMuligeMottakere_hovedMottakerVirksomhet_returnererVirksomhetSomHovedMottaker() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe GENERELT_FRITEKSTBREV_VIRKSOMHET.beskrivelse
        hovedMottaker.mottakerNavn shouldBe "Equinor AS"
        hovedMottaker.rolle shouldBe VIRKSOMHET
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun hentMuligeMottakere_hovedMottakerArbeidsgiver_returnererArbeidsgiverSomHovedMottaker() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
        hovedMottaker.mottakerNavn shouldBe "Ola Nordmann Rørleggerfirma"
        hovedMottaker.rolle shouldBe ARBEIDSGIVER
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        muligeMottakere.kopiMottakere.shouldBeEmpty()
        muligeMottakere.fasteMottakere.shouldBeEmpty()
    }

    @Test
    fun hentMuligeMottakere_kopiTilBruker_returnererBrukerSomKopi() {
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
        val kopiMottaker = kopiMottakere.first()
        kopiMottaker.dokumentNavn shouldBe "Kopi til bruker"
        kopiMottaker.mottakerNavn shouldBe "Ola Nordmann"
        kopiMottaker.rolle shouldBe BRUKER
        kopiMottaker.aktørId shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
        kopiMottaker.orgnr shouldBe null
    }

    @Test
    fun hentMuligeMottakere_kopiTilBrukerMedFullmektig_returnererFullmektigSomKopi() {
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

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 1
        val kopiMottaker = kopiMottakere.first()
        kopiMottaker.dokumentNavn shouldBe "Kopi til brukers fullmektig"
        kopiMottaker.mottakerNavn shouldBe "Fullmektig Virksomhet"
        kopiMottaker.rolle shouldBe FULLMEKTIG
        kopiMottaker.aktørId shouldBe null
        kopiMottaker.orgnr shouldBe "orgnrTilFullmektig"
    }

    @Test
    fun hentMuligeMottakere_kopiTilBrukerMedFullmektigNårHovedMottakerErBruker_returnererBrukerSomKopi() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe MANGELBREV_BRUKER.beskrivelse
        hovedMottaker.mottakerNavn shouldBe "Fullmektig Virksomhet"
        hovedMottaker.rolle shouldBe BRUKER
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 1
        val kopiMottaker = kopiMottakere.first()
        kopiMottaker.dokumentNavn shouldBe "Kopi til bruker"
        kopiMottaker.mottakerNavn shouldBe "Ola Nordmann"
        kopiMottaker.rolle shouldBe BRUKER
        kopiMottaker.aktørId shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
        kopiMottaker.orgnr shouldBe null
    }

    @Test
    fun hentMuligeMottakere_kopiTilArbeidsgiver_returnererArbeidsgiverSomKopi() {
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
        val kopiMottaker1 = kopiMottakere[0]
        kopiMottaker1.dokumentNavn shouldBe "Kopi til arbeidsgiver"
        kopiMottaker1.mottakerNavn shouldBe "Arbeidsgiver 1"
        kopiMottaker1.rolle shouldBe ARBEIDSGIVER
        kopiMottaker1.aktørId shouldBe null
        kopiMottaker1.orgnr shouldBe "orgnr1"

        val kopiMottaker2 = kopiMottakere[1]
        kopiMottaker2.dokumentNavn shouldBe "Kopi til arbeidsgiver"
        kopiMottaker2.mottakerNavn shouldBe "Arbeidsgiver 2"
        kopiMottaker2.rolle shouldBe ARBEIDSGIVER
        kopiMottaker2.aktørId shouldBe null
        kopiMottaker2.orgnr shouldBe "orgnr2"
    }

    @Test
    fun hentMuligeMottakere_kopiTilArbeidsgiverMedFullmektig_returnererFullmektigSomKopi() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(123L) } returns behandling
        every { brevmottakerService.hentMottakerliste(MANGELBREV_BRUKER, 123) } returns Mottakerliste(
            BRUKER,
            emptyList(),
            listOf(ARBEIDSGIVER),
            emptyList()
        )
        every { brevmottakerService.avklarMottaker(eq(MANGELBREV_BRUKER), any(), eq(behandling)) } returns lagMottakerOrg(BRUKER, null)
        val orgnr = lagMottakerOrg(FULLMEKTIG, "orgnr")
        every { brevmottakerService.avklarMottakere(eq(MANGELBREV_BRUKER), any(), eq(behandling), any(), any()) } returns listOf(orgnr)
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
                orgnr,
                "Kopi til arbeidsgivers fullmektig"
            )
        } returns "Kopi til arbeidsgivers fullmektig"

        val hentMottakereRequest = HentMuligeBrevmottakereService.RequestDto(MANGELBREV_BRUKER, 123L, null, null)

        val muligeMottakere = hentMuligeBrevmottakere.hentMuligeBrevmottakere(hentMottakereRequest)

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 1
        val kopiMottaker = kopiMottakere.first()
        kopiMottaker.dokumentNavn shouldBe "Kopi til arbeidsgivers fullmektig"
        kopiMottaker.mottakerNavn shouldBe "Fullmektig Virksomhet"
        kopiMottaker.rolle shouldBe FULLMEKTIG
        kopiMottaker.aktørId shouldBe null
        kopiMottaker.orgnr shouldBe "orgnr"
    }

    @Test
    fun hentMuligeMottakere_fastTilSkatt_returnererSkattSomFast() {
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
        val fastMottaker = fasteMottakere.first()
        fastMottaker.dokumentNavn shouldBe "Kopi til Skatteetaten"
        fastMottaker.mottakerNavn shouldBe "Skatteetaten"
        fastMottaker.rolle shouldBe NORSK_MYNDIGHET
        fastMottaker.aktørId shouldBe null
        fastMottaker.orgnr shouldBe "974761076"
    }

    @Test
    fun hentMuligeMottakere_hovedMottakerBruker_storbritanniaArtikkelUlik82() {
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

        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe "Vedtak om medlemskap, Attest for medlemskap i folketrygden"
        hovedMottaker.mottakerNavn shouldBe "Ola Nordmann"
        hovedMottaker.rolle shouldBe BRUKER
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null

        val kopiMottakere = muligeMottakere.kopiMottakere()
        kopiMottakere shouldHaveSize 2

        val fasteMottakere = muligeMottakere.fasteMottakere()
        fasteMottakere shouldHaveSize 1
        val fastMottaker = fasteMottakere.first()
        fastMottaker.dokumentNavn shouldBe "Kopi av vedtak om medlemskap"
        fastMottaker.mottakerNavn shouldBe "Skatt"
    }

    @Test
    fun hentMuligeMottakere_hovedMottakerUtenlandskTrygdemyndighet_HenterNavnFraOppgittInstitusjonID() {
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
        val hovedMottaker = muligeMottakere.hovedMottaker()
        hovedMottaker.dokumentNavn shouldBe "Fritekstbrev"
        hovedMottaker.mottakerNavn shouldBe "PT Operations"
        hovedMottaker.rolle shouldBe UTENLANDSK_TRYGDEMYNDIGHET
        hovedMottaker.aktørId shouldBe null
        hovedMottaker.orgnr shouldBe null
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

    private fun lagBehandling(): Behandling {
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medFagsak(lagFagsak())
            .build()
        behandling.saksopplysninger.add(lagPERSOPLSaksopplysning())
        return behandling
    }

    private fun lagFagsak(): Fagsak {
        return FagsakTestFactory.builder().medBruker().build()
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
            gyldighetsperiode = Periode(LocalDate.MIN, LocalDate.MAX)
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

    private fun lagPERSOPLSaksopplysning(): Saksopplysning {
        val dokument = PersonDokument().apply {
            fnr = "12345678910"
            sammensattNavn = "Ola Nordmann"
            gjeldendePostadresse.adresselinje1 = "Gateadresse 43A"
            gjeldendePostadresse.postnr = "0123"
            gjeldendePostadresse.land = Land.av(Land.NORGE)
        }
        return Saksopplysning().apply {
            this.dokument = dokument
            type = SaksopplysningType.PERSOPL
        }
    }
}
