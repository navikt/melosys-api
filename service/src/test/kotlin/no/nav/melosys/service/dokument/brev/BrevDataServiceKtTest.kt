package no.nav.melosys.service.dokument.brev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dok.brevdata.felles.v1.navfelles.*
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode
import no.nav.melosys.domain.*
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.brev.NorskMyndighet
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.kodeverk.*
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
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrevDataServiceKtTest {
    @Mock
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @Mock
    private lateinit var saksbehandlerService: SaksbehandlerService

    @Mock
    private lateinit var persondataFasade: PersondataFasade

    @Mock
    private lateinit var utenlandskMyndighetRepository: UtenlandskMyndighetRepository

    private lateinit var service: BrevDataService

    private val FNR = "Fnr"
    private val ORGNR = "Org-Nr"
    private val REP_ORGNR = "REP_Org-Nr"
    private val REP_FNR = "REP_Fnr"
    private val AKTØRID = "Aktør-Id"
    private val INSTITUSJON_ID = "HR:Zxcd"

    private val sammensattNavn = "ALTFOR SAMMENSATT"

    @BeforeEach
    fun setUp() {
        service = spy(BrevDataService(behandlingsresultatRepository, persondataFasade, saksbehandlerService, utenlandskMyndighetRepository))

        `when`(behandlingsresultatRepository.findById(anyLong())).thenReturn(java.util.Optional.of(Behandlingsresultat()))
        `when`(saksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Joe Moe")
        `when`(persondataFasade.hentFolkeregisterident(any())).thenReturn(FNR)
        `when`(persondataFasade.hentSammensattNavn(anyString())).thenReturn(sammensattNavn)
        lagUtenlandskMyndighet()
    }

    private fun lagUtenlandskMyndighet(): UtenlandskMyndighet {
        val myndighet = UtenlandskMyndighet()
        myndighet.navn = "navn"
        myndighet.gateadresse1 = "gateadresse 123"
        myndighet.gateadresse2 = "institusjon ABC"
        myndighet.land = "HR"
        `when`(utenlandskMyndighetRepository.findByLandkode(Land_iso2.HR)).thenReturn(java.util.Optional.of(myndighet))
        return myndighet
    }

    @Test
    fun lagA1_tilUtenlandskMyndighet() {
        val behandling = lagBehandling(lagSøknadDokument())
        val aktoerMyndighet = lagAktoerMyndighet()
        behandling.fagsak.leggTilAktør(aktoerMyndighet)
        val brevData = BrevDataVedlegg("Z123456")
        val myndighet = lagUtenlandskMyndighet()
        val mottakerMyndighet = lagMottakerMyndighet()
        val metadata = service.lagBestillingMetadata(
            ATTEST_A1, mottakerMyndighet, null,
            behandling, brevData
        )

        metadata.brukerID shouldBe FNR
        metadata.mottakerID shouldBe INSTITUSJON_ID
        metadata.utenlandskMyndighet shouldBe myndighet
        metadata.brukerNavn shouldBe sammensattNavn

        val element = service.lagBrevXML(ATTEST_A1, mottakerMyndighet, null, behandling, brevData)

        element.shouldNotBeNull()
    }

    @Test
    fun lagBrevXML_tilUtenlandskMyndighet() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevDataVedlegg("Z123456")
        val mottakerNorskMyndighet = no.nav.melosys.domain.brev.Mottaker.av(NorskMyndighet.SKATTEETATEN)
        val metadata = service.lagBestillingMetadata(
            ATTEST_A1, mottakerNorskMyndighet, null,
            behandling, brevData
        )

        metadata.mottakerID shouldBe mottakerNorskMyndighet.orgnr

        val element = service.lagBrevXML(ATTEST_A1, mottakerNorskMyndighet, null, behandling, brevData)

        element.shouldNotBeNull()
    }

    private fun lagAktoerMyndighet(): Aktoer {
        val myndighet = Aktoer()
        myndighet.rolle = Aktoersroller.TRYGDEMYNDIGHET
        myndighet.institusjonID = INSTITUSJON_ID
        return myndighet
    }

    private fun lagMottakerMyndighet(): no.nav.melosys.domain.brev.Mottaker {
        val myndighet = no.nav.melosys.domain.brev.Mottaker()
        myndighet.rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        myndighet.institusjonID = INSTITUSJON_ID
        return myndighet
    }

    @Test
    fun hentUtenlandskTrygdemyndighetFraMottaker() {
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        mottaker.institusjonID = "DE:TEST"
        val tyskMyndighet = UtenlandskMyndighet()
        tyskMyndighet.institusjonskode = "TEST"
        `when`(utenlandskMyndighetRepository.findByLandkode(Land_iso2.DE)).thenReturn(java.util.Optional.of(tyskMyndighet))

        val utenlandskMyndighet = service.hentUtenlandskTrygdemyndighetFraMottaker(mottaker)

        utenlandskMyndighet.institusjonskode shouldBe tyskMyndighet.institusjonskode
    }

    private fun lagMottaker(rolle: Mottakerroller): no.nav.melosys.domain.brev.Mottaker {
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = rolle
        when (rolle) {
            Mottakerroller.BRUKER -> mottaker.aktørId = AKTØRID
            Mottakerroller.ARBEIDSGIVER, Mottakerroller.VIRKSOMHET, Mottakerroller.NORSK_MYNDIGHET -> mottaker.orgnr = ORGNR
            Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> mottaker.institusjonID = "HR:987"
            Mottakerroller.FULLMEKTIG -> throw IllegalArgumentException("Bruk lagMottakerFullmektig() for fullmekitg mottaker")
        }
        return mottaker
    }

    private fun lagMottakerFullmektig(mottakerType: Aktoertype): no.nav.melosys.domain.brev.Mottaker {
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.FULLMEKTIG
        when (mottakerType) {
            Aktoertype.PERSON -> mottaker.personIdent = REP_FNR
            Aktoertype.ORGANISASJON -> mottaker.orgnr = REP_ORGNR
            else -> throw IllegalArgumentException("Mottakertype må være person eller organisasjon")
        }
        return mottaker
    }

    @Test
    fun lagMetadataForInnvilgelsesbrevAngirDokTypeLikInnvilgelseYrkesaktiv() {
        testLagDokumentMetadata(INNVILGELSE_YRKESAKTIV, Mottakerroller.BRUKER)
    }

    @Test
    fun lagMetadataForInnvilgelseArbeidsgiverBrevAngirDokTypeLikArbeidsgiver() {
        testLagDokumentMetadata(INNVILGELSE_ARBEIDSGIVER, Mottakerroller.ARBEIDSGIVER)
    }

    @Test
    fun avklarMottakerId_fullmektigOgKontaktOpplysningFinnes_kontaktOpplysningForFullmektigBrukes() {
        val behandling = lagBehandling(lagSøknadDokument())
        behandling.fagsak.leggTilAktør(hentFullmektigOrgAktør())

        val kontaktopplysning = Kontaktopplysning()
        kontaktopplysning.kontaktopplysningID = KontaktopplysningID("MELTEST-1", "999")
        kontaktopplysning.kontaktNavn = "brev motakker"
        kontaktopplysning.kontaktOrgnr = "KONTAKTORG_999"

        val brevData = BrevData("Z123456", "test", null)
        val mottaker = lagMottakerFullmektig(Aktoertype.ORGANISASJON)

        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            kontaktopplysning, behandling, brevData
        )

        metadata.brukerID shouldBe FNR
        metadata.mottakerID shouldBe "KONTAKTORG_999"

        val metadata2 = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker, null, behandling, brevData)

        metadata2.brukerID shouldBe FNR
        metadata2.mottakerID shouldBe REP_ORGNR
    }

    @Test
    fun lagBestillingMetadata_medBrukerMottakerOgBrukerUtenAdresseIRegister_skalHaBrukernavnOgPostadresse() {
        `when`(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser())

        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevData("Z123456", null, null)

        val mottaker = lagMottaker(Mottakerroller.BRUKER)
        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData
        )
        metadata.postadresse!!.gatenavn shouldBe "Strukturert Gate"
        metadata.brukerNavn shouldBe sammensattNavn
        metadata.berik shouldBe false
    }

    @Test
    fun lagBestillingMetadata_medBrukerMedAdresseIRegister_skalIkkeHaBrukerNavnEllerPostAdresse() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevData("Z123456", null, null)
        val mottaker = lagMottaker(Mottakerroller.BRUKER)
        `when`(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger())

        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData
        )
        metadata.postadresse shouldBe null
        metadata.brukerNavn shouldBe null
        metadata.berik shouldBe true
    }

    @Test
    fun lagBestillingMetadata_medUtenlandskMyndighet_skalUtfyllesMedBrukerNavn() {
        val behandling = lagBehandling(lagSøknadDokument())
        val brevData = BrevData("Z123456", null, null)

        val mottaker = lagMottaker(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, behandling, brevData
        )
        metadata.postadresse shouldBe null
        metadata.brukerNavn shouldBe sammensattNavn
        metadata.utenlandskMyndighet.shouldNotBeNull()
        metadata.berik shouldBe false
    }

    @Test
    fun lagBestillingMetadata_medNorskMyndighet_skalSeteOrgnrSomMottakerID() {
        val mottaker = lagMottaker(Mottakerroller.NORSK_MYNDIGHET)

        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            null, lagBehandling(lagSøknadDokument()), BrevData("Z123456", null, null)
        )

        metadata.mottakerID shouldBe mottaker.orgnr
    }

    @Test
    fun avklarMottakerId_ingenFullmektigForArbeidsgiverOgKontaktOpplysningFinnes_kontaktOpplysningBrukes() {
        val kontaktopplysning = Kontaktopplysning()
        kontaktopplysning.kontaktopplysningID = KontaktopplysningID("MELTEST-1", "999")
        kontaktopplysning.kontaktNavn = "brev motakker"
        kontaktopplysning.kontaktOrgnr = "KONTAKTORG_999"

        val brevData = BrevData("Z123456", null, null)
        val mottaker = lagMottaker(Mottakerroller.ARBEIDSGIVER)
        brevData.fritekst = "Test"

        val behandling = lagBehandling(lagSøknadDokument())
        val metadata = service.lagBestillingMetadata(
            INNVILGELSE_YRKESAKTIV, mottaker,
            kontaktopplysning, behandling, brevData
        )
        metadata.mottakerID shouldBe "KONTAKTORG_999"

        val metadata2 = service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, mottaker, null, behandling, brevData)
        metadata2.mottakerID shouldBe ORGNR
        metadata2.berik shouldBe true
    }

    @Test
    fun lagMottaker_bruker_riktigeVerdier() {
        `when`(persondataFasade.hentPerson(AKTØRID)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger())
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.BRUKER
        mottaker.aktørId = AKTØRID

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Person()
        expectedBrevMottaker.typeKode = AktoerType.PERSON
        expectedBrevMottaker.spraakkode = Spraakkode.NB
        expectedBrevMottaker.id = FNR
        expectedBrevMottaker.mottakeradresse = lagPlassholderAdresse()
        expectedBrevMottaker.berik = true
        expectedBrevMottaker.navn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.kortNavn = BrevDataService.PLASSHOLDER_TEKST

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun lagMottaker_arbeidsgiver_riktigeVerdier() {
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.ARBEIDSGIVER
        mottaker.orgnr = ORGNR

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Organisasjon()
        expectedBrevMottaker.id = ORGNR
        expectedBrevMottaker.typeKode = AktoerType.ORGANISASJON
        expectedBrevMottaker.navn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.kortNavn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.mottakeradresse = lagPlassholderAdresse()
        expectedBrevMottaker.spraakkode = Spraakkode.NB

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun lagMottaker_trygdemyndighetUtenlandsk_riktigeVerdier() {
        val mottaker = lagMottakerMyndighet()

        val brevMottaker = service.lagMottaker(mottaker, null)

        val myndighet = lagUtenlandskMyndighet()
        val expectedBrevMottaker = Person()
        expectedBrevMottaker.id = INSTITUSJON_ID
        expectedBrevMottaker.typeKode = AktoerType.PERSON
        expectedBrevMottaker.berik = false
        expectedBrevMottaker.navn = myndighet.navn
        expectedBrevMottaker.kortNavn = myndighet.navn
        expectedBrevMottaker.spraakkode = Spraakkode.NB
        expectedBrevMottaker.mottakeradresse = UtenlandskPostadresse()
            .withAdresselinje1(myndighet.gateadresse1)
            .withAdresselinje2(myndighet.gateadresse2)
            .withAdresselinje3(myndighet.postnummer + " " + myndighet.poststed)
            .withLand(myndighet.land)

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun lagMottaker_trygdemyndighetIkkeUtenlandsk_riktigeVerdier() {
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
        mottaker.orgnr = ORGNR

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Organisasjon()
        expectedBrevMottaker.id = ORGNR
        expectedBrevMottaker.typeKode = AktoerType.ORGANISASJON
        expectedBrevMottaker.navn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.kortNavn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.mottakeradresse = lagPlassholderAdresse()
        expectedBrevMottaker.spraakkode = Spraakkode.NB

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun lagMottaker_fullmektigPerson_riktigeVerdier() {
        `when`(persondataFasade.hentPerson(REP_FNR)).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger())
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.FULLMEKTIG
        mottaker.personIdent = REP_FNR

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Person()
        expectedBrevMottaker.typeKode = AktoerType.PERSON
        expectedBrevMottaker.spraakkode = Spraakkode.NB
        expectedBrevMottaker.id = REP_FNR
        expectedBrevMottaker.mottakeradresse = lagPlassholderAdresse()
        expectedBrevMottaker.berik = true
        expectedBrevMottaker.navn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.kortNavn = BrevDataService.PLASSHOLDER_TEKST

        brevMottaker shouldBe expectedBrevMottaker
    }

    @Test
    fun lagMottaker_fullmektigOrganisasjon_riktigeVerdier() {
        val mottaker = no.nav.melosys.domain.brev.Mottaker()
        mottaker.rolle = Mottakerroller.FULLMEKTIG
        mottaker.orgnr = REP_ORGNR

        val brevMottaker = service.lagMottaker(mottaker, null)

        val expectedBrevMottaker = Organisasjon()
        expectedBrevMottaker.id = REP_ORGNR
        expectedBrevMottaker.typeKode = AktoerType.ORGANISASJON
        expectedBrevMottaker.navn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.kortNavn = BrevDataService.PLASSHOLDER_TEKST
        expectedBrevMottaker.mottakeradresse = lagPlassholderAdresse()
        expectedBrevMottaker.spraakkode = Spraakkode.NB

        brevMottaker shouldBe expectedBrevMottaker
    }

    private fun testLagDokumentMetadata(doktype: Produserbaredokumenter, rolle: Mottakerroller) {
        `when`(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger())
        testLagDokumentMetadata(doktype, lagMottaker(rolle), rolle)
    }

    private fun testLagDokumentMetadata(doktype: Produserbaredokumenter, mottaker: no.nav.melosys.domain.brev.Mottaker, rolle: Mottakerroller) {
        val resultat = service.lagBestillingMetadata(
            doktype, mottaker, null,
            lagBehandling(lagSøknadDokument()), lagBrevData()
        )
        val forventet = lagDokumentbestillingMetadata(doktype, rolle)
        resultat shouldBe forventet
    }

    private fun lagDokumentbestillingMetadata(
        doktype: Produserbaredokumenter,
        rolle: Mottakerroller
    ): DokumentbestillingMetadata {
        val forventet = DokumentbestillingMetadata()
        forventet.brukerID = FNR
        forventet.mottaker = lagMottaker(rolle)
        if (rolle == Mottakerroller.BRUKER) {
            forventet.mottakerID = FNR
        } else {
            forventet.mottakerID = ORGNR
        }

        forventet.dokumenttypeID = DokumenttypeIdMapper.hentID(doktype)
        forventet.fagområde = "MED"
        forventet.journalsakID = FagsakTestFactory.GSAK_SAKSNUMMER.toString()
        forventet.saksbehandler = "TEST"
        forventet.berik = true

        return forventet
    }

    private fun lagBrevData(): BrevData {
        val brevDataDto = BrevData()
        brevDataDto.saksbehandler = "TEST"
        brevDataDto.fritekst = "Test"
        return brevDataDto
    }

    private fun lagBehandling(mottatteOpplysningerData: MottatteOpplysningerData): Behandling {
        val bruker = Aktoer()
        bruker.aktørId = AKTØRID
        bruker.rolle = Aktoersroller.BRUKER

        val arbeidsgiver = Aktoer()
        arbeidsgiver.orgnr = ORGNR
        arbeidsgiver.rolle = Aktoersroller.ARBEIDSGIVER

        val fagsak = FagsakTestFactory.builder().medGsakSaksnummer().aktører(setOf(bruker, arbeidsgiver)).build()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medRegistrertDato(java.time.Instant.now())
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(MottatteOpplysninger())
            .build()

        behandling.mottatteOpplysninger.mottatteOpplysningerData = mottatteOpplysningerData

        return behandling
    }

    private fun hentFullmektigOrgAktør(): Aktoer {
        val aktørArbFullmektig = Aktoer()
        aktørArbFullmektig.rolle = Aktoersroller.FULLMEKTIG
        aktørArbFullmektig.orgnr = REP_ORGNR
        aktørArbFullmektig.fullmaktstype = Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER
        return aktørArbFullmektig
    }

    private fun lagSøknadDokument(): Soeknad {
        val søknad = Soeknad()
        søknad.bosted.oppgittAdresse = lagStrukturertAdresse()
        return søknad
    }

    private fun lagPlassholderAdresse(): NorskPostadresse {
        return NorskPostadresse()
            .withAdresselinje1(BrevDataService.PLASSHOLDER_TEKST)
            .withPostnummer(BrevDataService.PLASSHOLDER_POSTNUMMER)
            .withPoststed(BrevDataService.PLASSHOLDER_TEKST)
            .withLand(BrevDataService.PLASSHOLDER_TEKST)
    }
}
