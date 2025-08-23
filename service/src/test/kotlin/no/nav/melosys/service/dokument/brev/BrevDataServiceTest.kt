package no.nav.melosys.service.dokument.brev

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import no.nav.dok.brevdata.felles.v1.navfelles.NorskPostadresse
import no.nav.dok.brevdata.felles.v1.navfelles.Organisasjon
import no.nav.dok.brevdata.felles.v1.navfelles.Person
import no.nav.dok.brevdata.felles.v1.navfelles.UtenlandskPostadresse
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.UtenlandskMyndighetRepository
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.*

@ExtendWith(MockKExtension::class)
class BrevDataServiceTest {

    @MockK
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK
    private lateinit var saksbehandlerService: SaksbehandlerService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var utenlandskMyndighetRepository: UtenlandskMyndighetRepository

    private lateinit var service: BrevDataService


    @BeforeEach
    fun setUp() {
        service = spyk(
            BrevDataService(
                behandlingsresultatRepository,
                persondataFasade,
                saksbehandlerService,
                utenlandskMyndighetRepository
            )
        )

        every { behandlingsresultatRepository.findById(any()) } returns Optional.of(Behandlingsresultat())
        every { saksbehandlerService.hentNavnForIdent(any()) } returns "Joe Moe"
        every { persondataFasade.hentFolkeregisterident(any()) } returns FNR
        every { persondataFasade.hentSammensattNavn(any()) } returns sammensattNavn
        lagUtenlandskMyndighet()
    }

    private fun lagUtenlandskMyndighet(): UtenlandskMyndighet {
        val myndighet = UtenlandskMyndighet().apply {
            navn = "navn"
            gateadresse1 = "gateadresse 123"
            gateadresse2 = "institusjon ABC"
            land = "HR"
        }
        every { utenlandskMyndighetRepository.findByLandkode(Land_iso2.HR) } returns Optional.of(myndighet)
        return myndighet
    }

    private fun lagBehandling(mottatteOpplysningerData: MottatteOpplysningerData): Behandling {
        val bruker = Aktoer().apply {
            aktørId = AKTØRID
            rolle = Aktoersroller.BRUKER
        }

        val arbeidsgiver = Aktoer().apply {
            orgnr = ORGNR
            rolle = Aktoersroller.ARBEIDSGIVER
        }

        val mottatteOppl = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }

        return Behandling.forTest {
            id = 1L
            registrertDato = Instant.now()
            type = Behandlingstyper.FØRSTEGANG
            mottatteOpplysninger = mottatteOppl
            fagsak {
                medGsakSaksnummer()
                aktører(setOf(bruker, arbeidsgiver))
            }
        }
    }

    private fun lagSøknadDokument(): Soeknad = Soeknad().apply {
        bosted.oppgittAdresse = lagStrukturertAdresse()
    }

    private fun lagMottaker(rolle: Mottakerroller): Mottaker = Mottaker().apply {
        this.rolle = rolle
        when (rolle) {
            Mottakerroller.BRUKER -> aktørId = AKTØRID
            Mottakerroller.ARBEIDSGIVER, Mottakerroller.VIRKSOMHET, Mottakerroller.NORSK_MYNDIGHET -> orgnr = ORGNR
            Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> institusjonID = "HR:987"
            Mottakerroller.FULLMEKTIG -> throw IllegalArgumentException("Bruk lagMottakerFullmektig() for fullmektig mottaker")
            else -> {}
        }
    }

    private fun lagMottakerFullmektig(mottakerType: Aktoertype): Mottaker = Mottaker().apply {
        rolle = Mottakerroller.FULLMEKTIG
        when (mottakerType) {
            Aktoertype.PERSON -> personIdent = REP_FNR
            Aktoertype.ORGANISASJON -> orgnr = REP_ORGNR
            else -> throw IllegalArgumentException("Mottakertype må være person eller organisasjon")
        }
    }

    private fun hentFullmektigOrgAktør(): Aktoer = Aktoer().apply {
        rolle = Aktoersroller.FULLMEKTIG
        orgnr = REP_ORGNR
        setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
    }

    private fun lagPlassholderAdresse() = NorskPostadresse()
        .withAdresselinje1(BrevDataService.PLASSHOLDER_TEKST)
        .withPostnummer(BrevDataService.PLASSHOLDER_POSTNUMMER)
        .withPoststed(BrevDataService.PLASSHOLDER_TEKST)
        .withLand(BrevDataService.PLASSHOLDER_TEKST)

    private fun testLagDokumentMetadata(doktype: Produserbaredokumenter, rolle: Mottakerroller) {
        testLagDokumentMetadata(doktype, lagMottaker(rolle), rolle)
    }

    private fun testLagDokumentMetadata(doktype: Produserbaredokumenter, mottaker: Mottaker, rolle: Mottakerroller) {
        val resultat = service.lagBestillingMetadata(
            doktype, mottaker, null,
            lagBehandling(lagSøknadDokument()), lagBrevData()
        )
        val forventet = lagDokumentbestillingMetadata(doktype, rolle)
        resultat shouldBe forventet
    }

    private fun lagDokumentbestillingMetadata(doktype: Produserbaredokumenter, rolle: Mottakerroller) = DokumentbestillingMetadata().apply {
        brukerID = FNR
        mottaker = lagMottaker(rolle)
        mottakerID = if (rolle == Mottakerroller.BRUKER) FNR else ORGNR
        dokumenttypeID = DokumenttypeIdMapper.hentID(doktype)
        fagområde = "MED"
        journalsakID = FagsakTestFactory.GSAK_SAKSNUMMER.toString()
        saksbehandler = "TEST"
        berik = true
    }

    private fun lagBrevData(): BrevData = BrevData().apply {
        saksbehandler = "TEST"
        fritekst = "Test"
    }

    private fun lagAktoerMyndighet(): Aktoer = Aktoer().apply {
        rolle = Aktoersroller.TRYGDEMYNDIGHET
        institusjonID = INSTITUSJON_ID
    }

    private fun lagMottakerMyndighet(): Mottaker = Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET).apply {
        institusjonID = INSTITUSJON_ID
    }

    @Test
    fun `lag A1 til utenlandsk myndighet`() {
        val behandling = lagBehandling(lagSøknadDokument())
        val aktoerMyndighet = lagAktoerMyndighet()
        behandling.fagsak.leggTilAktør(aktoerMyndighet)
        val brevData = BrevDataVedlegg("Z123456")
        val myndighet = lagUtenlandskMyndighet()
        val mottakerMyndighet = lagMottakerMyndighet()

        val metadata = service.lagBestillingMetadata(
            ATTEST_A1,
            mottakerMyndighet,
            null,
            behandling,
            brevData
        )

        metadata.brukerID shouldBe FNR
        metadata.mottakerID shouldBe INSTITUSJON_ID
        metadata.utenlandskMyndighet shouldBe myndighet
        metadata.brukerNavn shouldBe sammensattNavn

        val element = service.lagBrevXML(ATTEST_A1, mottakerMyndighet, null, behandling, brevData)

        element.shouldNotBeNull()
    }

    @Test
    fun `skal returnere utenlandsk myndighet når mottaker har gyldig institusjonID`() {
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
            institusjonID = "DE:TEST"
        }
        val tyskMyndighet = UtenlandskMyndighet().apply {
            institusjonskode = "TEST"
        }
        every { utenlandskMyndighetRepository.findByLandkode(Land_iso2.DE) } returns Optional.of(tyskMyndighet)

        val utenlandskMyndighet = service.hentUtenlandskTrygdemyndighetFraMottaker(mottaker)

        utenlandskMyndighet.institusjonskode shouldBe tyskMyndighet.institusjonskode
    }

    @Test
    fun `lag BrevXML til norsk myndighet`() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevDataVedlegg("Z123456")
        val mottakerNorskMyndighet = Mottaker.av(NorskMyndighet.SKATTEETATEN)

        val metadata = service.lagBestillingMetadata(
            ATTEST_A1,
            mottakerNorskMyndighet,
            null,
            behandling,
            brevData
        )

        metadata.mottakerID shouldBe mottakerNorskMyndighet.orgnr

        val element = service.lagBrevXML(ATTEST_A1, mottakerNorskMyndighet, null, behandling, brevData)

        element.shouldNotBeNull()
    }

    @Test
    fun `innvilgelse yrkesaktiv skal ha doktype innvilgelse og bruker som mottaker`() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        testLagDokumentMetadata(INNVILGELSE_YRKESAKTIV, Mottakerroller.BRUKER)
    }

    @Test
    fun `innvilgelse arbeidsgiver skal ha doktype innvilgelse og arbeidsgiver som mottaker`() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        testLagDokumentMetadata(INNVILGELSE_ARBEIDSGIVER, Mottakerroller.ARBEIDSGIVER)
    }

    @Test
    fun `avklarMottakerId fullmektigOgKontaktOpplysningFinnes kontaktOpplysningForFullmektigBrukes`() {
        val behandling = lagBehandling(lagSøknadDokument())
        behandling.fagsak.leggTilAktør(hentFullmektigOrgAktør())

        val kontaktopplysning = Kontaktopplysning().apply {
            kontaktopplysningID = KontaktopplysningID("MELTEST-1", "999")
            kontaktNavn = "brev motakker"
            kontaktOrgnr = "KONTAKTORG_999"
        }

        val brevData = BrevData("Z123456", "test", null)
        val mottaker = lagMottakerFullmektig(Aktoertype.ORGANISASJON)

        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            kontaktopplysning, behandling, brevData
        )

        metadata.run {
            brukerID shouldBe FNR
            mottakerID shouldBe "KONTAKTORG_999"
        }

        val metadata2 = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker, null, behandling, brevData
        )

        metadata2.run {
            brukerID shouldBe FNR
            mottakerID shouldBe REP_ORGNR
        }
    }

    @Test
    fun `lagBestillingMetadata medBrukerMottakerOgBrukerUtenAdresseIRegister skalHaBrukernavnOgPostadresse`() {
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()

        val søknad = lagSøknadDokument()
        søknad.bosted.oppgittAdresse = lagStrukturertAdresse()
        val behandling = lagBehandling(søknad)
        val brevData = BrevData("Z123456", null, null)

        val mottaker = lagMottaker(Mottakerroller.BRUKER)
        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData
        )
        metadata.run {
            postadresse?.gatenavn shouldBe "Strukturert Gate"
            brukerNavn shouldBe sammensattNavn
            berik?.shouldBeFalse()
        }
    }

    @Test
    fun `lagBestillingMetadata medBrukerMedAdresseIRegister skalIkkeHaBrukerNavnEllerPostAdresse`() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevData("Z123456", null, null)
        val mottaker = lagMottaker(Mottakerroller.BRUKER)
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()

        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData
        )
        metadata.run {
            postadresse.shouldBeNull()
            brukerNavn.shouldBeNull()
            berik?.shouldBeTrue()
        }
    }

    @Test
    fun `lagBestillingMetadata medUtenlandskMyndighet skalUtfyllesMedBrukerNavn`() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevData("Z123456", null, null)

        val mottaker = lagMottaker(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData
        )
        metadata.run {
            postadresse.shouldBeNull()
            brukerNavn shouldBe sammensattNavn
            utenlandskMyndighet.shouldNotBeNull()
            berik?.shouldBeFalse()
        }
    }

    @Test
    fun `lagBestillingMetadata medNorskMyndighet skalSeteOrgnrSomMottakerID`() {
        val mottaker = lagMottaker(Mottakerroller.NORSK_MYNDIGHET)

        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, lagBehandling(lagSøknadDokument()), BrevData("Z123456", null, null)
        )

        metadata.mottakerID shouldBe mottaker.orgnr
    }

    @Test
    fun `avklarMottakerId ingenFullmektigForArbeidsgiverOgKontaktOpplysningFinnes kontaktOpplysningBrukes`() {
        val kontaktopplysning = Kontaktopplysning().apply {
            kontaktopplysningID = KontaktopplysningID("MELTEST-1", "999")
            kontaktNavn = "brev motakker"
            kontaktOrgnr = "KONTAKTORG_999"
        }

        val brevData = BrevData("Z123456", null, null).apply {
            fritekst = "Test"
        }
        val mottaker = lagMottaker(Mottakerroller.ARBEIDSGIVER)

        val behandling = lagBehandling(lagSøknadDokument())
        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            kontaktopplysning, behandling, brevData
        )
        metadata.mottakerID shouldBe "KONTAKTORG_999"

        val metadata2 = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker, null, behandling, brevData
        )
        metadata2.run {
            mottakerID shouldBe ORGNR
            berik?.shouldBeTrue()
        }
    }

    @Test
    fun `lagMottaker bruker riktigeVerdier`() {
        every { persondataFasade.hentPerson(AKTØRID) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.BRUKER
            aktørId = AKTØRID
        }

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Person().apply {
            typeKode = AktoerType.PERSON
            spraakkode = Spraakkode.NB
            id = FNR
            mottakeradresse = lagPlassholderAdresse()
            navn = BrevDataService.PLASSHOLDER_TEKST
            kortNavn = BrevDataService.PLASSHOLDER_TEKST
        }.also { it.setBerik(true) }

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun `lagMottaker arbeidsgiver riktigeVerdier`() {
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.ARBEIDSGIVER
            orgnr = ORGNR
        }

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Organisasjon().apply {
            id = ORGNR
            typeKode = AktoerType.ORGANISASJON
            navn = BrevDataService.PLASSHOLDER_TEKST
            kortNavn = BrevDataService.PLASSHOLDER_TEKST
            mottakeradresse = lagPlassholderAdresse()
            spraakkode = Spraakkode.NB
        }

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun `lagMottaker trygdemyndighetUtenlandsk riktigeVerdier`() {
        val mottaker = lagMottakerMyndighet()

        val brevMottaker = service.lagMottaker(mottaker, null)

        val myndighet = lagUtenlandskMyndighet()
        val expectedBrevMottaker = Person().apply {
            id = INSTITUSJON_ID
            typeKode = AktoerType.PERSON
            navn = myndighet.navn
            kortNavn = myndighet.navn
            spraakkode = Spraakkode.NB
            mottakeradresse = UtenlandskPostadresse()
                .withAdresselinje1(myndighet.gateadresse1)
                .withAdresselinje2(myndighet.gateadresse2)
                .withAdresselinje3(myndighet.postnummer + " " + myndighet.poststed)
                .withLand(myndighet.land)
        }
        expectedBrevMottaker.setBerik(false)

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun `lagMottaker trygdemyndighetIkkeUtenlandsk riktigeVerdier`() {
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
            orgnr = ORGNR
        }

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Organisasjon().apply {
            id = ORGNR
            typeKode = AktoerType.ORGANISASJON
            navn = BrevDataService.PLASSHOLDER_TEKST
            kortNavn = BrevDataService.PLASSHOLDER_TEKST
            mottakeradresse = lagPlassholderAdresse()
            spraakkode = Spraakkode.NB
        }

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun `lagMottaker fullmektigPerson riktigeVerdier`() {
        every { persondataFasade.hentPerson(REP_FNR) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.FULLMEKTIG
            personIdent = REP_FNR
        }

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Person().apply {
            typeKode = AktoerType.PERSON
            spraakkode = Spraakkode.NB
            id = REP_FNR
            mottakeradresse = lagPlassholderAdresse()
            navn = BrevDataService.PLASSHOLDER_TEKST
            kortNavn = BrevDataService.PLASSHOLDER_TEKST
        }.also { it.setBerik(true) }

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun `lagMottaker fullmektigOrganisasjon riktigeVerdier`() {
        val mottaker = Mottaker().apply {
            rolle = Mottakerroller.FULLMEKTIG
            orgnr = REP_ORGNR
        }

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Organisasjon().apply {
            id = REP_ORGNR
            typeKode = AktoerType.ORGANISASJON
            navn = BrevDataService.PLASSHOLDER_TEKST
            kortNavn = BrevDataService.PLASSHOLDER_TEKST
            mottakeradresse = lagPlassholderAdresse()
            spraakkode = Spraakkode.NB
        }

        brevMottaker shouldBe expectedBrevMottaker
    }

    companion object {
        private const val FNR = "Fnr"
        private const val ORGNR = "Org-Nr"
        private const val REP_ORGNR = "REP_Org-Nr"
        private const val REP_FNR = "REP_Fnr"
        private const val AKTØRID = "Aktør-Id"
        private const val INSTITUSJON_ID = "HR:Zxcd"
        private const val sammensattNavn = "ALTFOR SAMMENSATT"
    }
}
