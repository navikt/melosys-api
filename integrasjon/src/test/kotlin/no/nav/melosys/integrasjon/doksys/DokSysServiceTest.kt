package no.nav.melosys.integrasjon.doksys

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.DistribuerJournalpostConsumer
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Dokumentbestillingsinformasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.UtenlandskPostadresse
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

@ExtendWith(MockKExtension::class)
class DokSysServiceTest {

    private val FNR = "12345678901"
    private val ORGNR = "98765432"
    private val INSITUSJON_ID = "DK:1234"
    private val REP_FNR = "10987654321"
    private val REP_ORGNR = "87654321"

    private val dokumentproduksjonConsumer = mockk<DokumentproduksjonConsumer>()
    private val distribuerJournalpostConsumer = mockk<DistribuerJournalpostConsumer>()

    private lateinit var dokSysService: DoksysService

    @BeforeEach
    fun setUp() {
        dokSysService = DoksysService(dokumentproduksjonConsumer, distribuerJournalpostConsumer)
    }

    @Test
    fun `produser dokumentutkast`() {
        val metadata = lagMetadataForBruker(null)
        every { dokumentproduksjonConsumer.produserDokumentutkast(any()) } returns ProduserDokumentutkastResponse()

        dokSysService.produserDokumentutkast(Dokumentbestilling(metadata, lagBrevData()))

        val captor = slot<ProduserDokumentutkastRequest>()
        verify { dokumentproduksjonConsumer.produserDokumentutkast(capture(captor)) }
        val dokumentutkastRequest = captor.captured
        dokumentutkastRequest.dokumenttypeId shouldBe metadata.dokumenttypeID
    }

    @Test
    fun `produser ikke-redigerbart dokument for bruker`() {
        val metadata = lagMetadataForBruker(null)
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            dokumenttypeId shouldBe metadata.dokumenttypeID
            adresse.shouldBeNull()
            mottaker.isBerik shouldBe true
            mottaker.shouldBeInstanceOf<Person>()
            bruker.shouldBeInstanceOf<Person>()
                .ident shouldBe FNR
        }
    }

    @Test
    fun `produser ikke-redigerbart dokument for bruker med postadresse gir postadresse og navn`() {
        val postadresse = StrukturertAdresse().apply {
            gatenavn = "Gatenavn"
            husnummerEtasjeLeilighet = "123"
            postnummer = "1337"
            poststed = "Poststed"
            region = "Region"
            landkode = "BE"
        }
        val metadata = lagMetadataForBruker(postadresse)
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            mottaker.isBerik shouldBe false

            adresse.shouldBeInstanceOf<UtenlandskPostadresse>().run {
                adresselinje1 shouldBe "${postadresse.gatenavn} ${postadresse.husnummerEtasjeLeilighet}"
                adresselinje2 shouldBe "${postadresse.postnummer} ${postadresse.poststed}"
                adresselinje3 shouldBe postadresse.region
                land.value shouldBe postadresse.landkode
            }
            bruker.shouldBeInstanceOf<Person>()
                .navn shouldBe "Kim Se"
        }
    }

    @Test
    fun `produser ikke-redigerbart dokument til arbeidsgiver`() {
        val metadata = lagMetadataMedArbeidsgiver()
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            mottaker.shouldBeInstanceOf<Organisasjon>()
                .orgnummer shouldBe ORGNR
        }
    }

    @Test
    fun `produser ikke-redigerbart dokument til fullmektig person`() {
        val metadata = lagMetadataMedFullmektig(Aktoertype.PERSON)
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            mottaker.isBerik shouldBe true
            mottaker.shouldBeInstanceOf<Person>()
                .ident shouldBe REP_FNR
        }
    }

    @Test
    fun `produser ikke-redigerbart dokument til fullmektig organisasjon`() {
        val metadata = lagMetadataMedFullmektig(Aktoertype.ORGANISASJON)
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            mottaker.shouldBeInstanceOf<Organisasjon>()
                .orgnummer shouldBe REP_ORGNR
        }
    }

    @Test
    fun `produser ikke-redigerbart dokument til utenlandsk myndighet`() {
        val metadata = lagMetadataMedUtenlandskMyndighet()
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            mottaker.isBerik shouldBe false
            mottaker.shouldBeInstanceOf<Person>()
                .navn shouldBe metadata.utenlandskMyndighet!!.navn

            adresse.shouldBeInstanceOf<UtenlandskPostadresse>().run {
                adresselinje1 shouldBe metadata.utenlandskMyndighet!!.gateadresse1
                land.value shouldBe metadata.utenlandskMyndighet!!.landkode.kode
            }
        }
    }

    @Test
    fun `produser ikke-redigerbart dokument til norsk myndighet`() {
        val metadata = lagMetadataMedNorskMyndighet()
        every { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any()) } returns ProduserIkkeredigerbartDokumentResponse()


        dokSysService.produserIkkeredigerbartDokument(Dokumentbestilling(metadata, lagBrevData()))


        val captor = slot<ProduserIkkeredigerbartDokumentRequest>()
        verify { dokumentproduksjonConsumer.produserIkkeredigerbartDokument(capture(captor)) }

        hentDokumentBestillingInfoFraCaptor(captor).run {
            mottaker.shouldBeInstanceOf<Organisasjon>()
                .orgnummer shouldBe metadata.mottakerID
        }
    }

    @Test
    fun `distribuer journalpost med norsk adresse`() {
        val mottakeradresse = StrukturertAdresse().apply {
            landkode = "NO"
            gatenavn = "gate"
            postnummer = "0463"
            region = "Oslo"
            husnummerEtasjeLeilighet = "4B"
            poststed = "Oslo"
        }
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost("123456", mottakeradresse, Distribusjonstype.VIKTIG)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe "123456"
            adresse.adresseType shouldBe "norskPostadresse"
            distribusjonstype shouldBe Distribusjonstype.VIKTIG
        }
    }

    @Test
    fun `distribuer journalpost med utenlandsk adresse`() {
        val mottakeradresse = StrukturertAdresse().apply {
            landkode = "SE"
            gatenavn = "svensk gate"
            postnummer = "9999"
            region = "Sverige"
            husnummerEtasjeLeilighet = "4B"
            poststed = "Stockholm"
        }
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost("123456", mottakeradresse, Distribusjonstype.VIKTIG)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe "123456"
            adresse.adresseType shouldBe "utenlandskPostadresse"
            distribusjonstype shouldBe Distribusjonstype.VIKTIG
        }
    }

    @Test
    fun `distribuer journalpost uten adresse`() {
        val journalpostId = "123456"
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost(journalpostId, Distribusjonstype.ANNET)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe journalpostId
            adresse.shouldBeNull()
            distribusjonstype shouldBe Distribusjonstype.ANNET
        }
    }

    @Test
    fun `distribuer journalpost med strukturert norsk adresse uten kontaktopplysning`() {
        val journalpostId = "123456"
        val strukturertAdresse = StrukturertAdresse().apply {
            gatenavn = "Postboks 222"
            postnummer = "9999"
            landkode = "NO"
        }
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, null, null, Distribusjonstype.VEDTAK)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe journalpostId
            adresse.adresseType shouldBe "norskPostadresse"
            adresse.adresselinje1 shouldBe strukturertAdresse.gatenavn
            adresse.postnummer shouldBe strukturertAdresse.postnummer
            distribusjonstype shouldBe Distribusjonstype.VEDTAK
        }
    }

    @Test
    fun `distribuer journalpost med strukturert utenlandsk adresse uten kontaktopplysning`() {
        val journalpostId = "123456"
        val strukturertAdresse = StrukturertAdresse().apply {
            gatenavn = "Postboks 222"
            postnummer = "9999"
            landkode = "BE"
        }
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, null, null, Distribusjonstype.VEDTAK)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe journalpostId
            adresse.adresseType shouldBe "utenlandskPostadresse"
            adresse.adresselinje1 shouldBe strukturertAdresse.gatenavn
            adresse.postnummer.shouldBeNull()
            distribusjonstype shouldBe Distribusjonstype.VEDTAK
        }
    }

    @Test
    fun `distribuer journalpost med strukturert norsk adresse med kontaktopplysning`() {
        val journalpostId = "123456"
        val strukturertAdresse = StrukturertAdresse().apply {
            gatenavn = "Postboks 222"
            postnummer = "9999"
            landkode = "NO"
        }
        val kontaktopplysning = Kontaktopplysning().apply {
            kontaktNavn = "Fetter Anton"
        }
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, kontaktopplysning, null, Distribusjonstype.VEDTAK)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe journalpostId
            adresse.adresseType shouldBe "norskPostadresse"
            adresse.adresselinje1 shouldBe "Att: Fetter Anton"
            adresse.adresselinje2 shouldBe strukturertAdresse.gatenavn
            adresse.postnummer shouldBe strukturertAdresse.postnummer
            distribusjonstype shouldBe Distribusjonstype.VEDTAK
        }
    }

    @Test
    fun `distribuer journalpost med strukturert norsk adresse med kontaktopplysning og overstyrt kontaktperson navn`() {
        val journalpostId = "123456"
        val strukturertAdresse = StrukturertAdresse().apply {
            gatenavn = "Postboks 222"
            postnummer = "9999"
            landkode = "NO"
        }
        val kontaktopplysning = Kontaktopplysning().apply {
            kontaktNavn = "Fetter Anton"
        }
        every { distribuerJournalpostConsumer.distribuerJournalpost(any<DistribuerJournalpostRequest>()) } returns DistribuerJournalpostResponse("123")


        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, kontaktopplysning, "Kari Kontakt", Distribusjonstype.ANNET)


        val captor = slot<DistribuerJournalpostRequest>()
        verify { distribuerJournalpostConsumer.distribuerJournalpost(capture(captor)) }

        captor.captured.run {
            journalpostId shouldBe journalpostId
            adresse.adresseType shouldBe "norskPostadresse"
            adresse.adresselinje1 shouldBe "Att: Kari Kontakt"
            adresse.adresselinje2 shouldBe strukturertAdresse.gatenavn
            adresse.postnummer shouldBe strukturertAdresse.postnummer
            distribusjonstype shouldBe Distribusjonstype.ANNET
        }
    }

    private fun hentDokumentBestillingInfoFraCaptor(captor: io.mockk.CapturingSlot<ProduserIkkeredigerbartDokumentRequest>): Dokumentbestillingsinformasjon =
        captor.captured.dokumentbestillingsinformasjon

    private fun lagMetadataForBruker(postadresse: StrukturertAdresse?) = DokumentbestillingMetadata().apply {
        dokumenttypeID = "dok_1234"
        brukerNavn = "Kim Se"
        brukerID = FNR
        mottaker = lagMottaker(Mottakerroller.BRUKER)
        postadresse?.let { this.postadresse = it } ?: run { berik = true }
    }

    private fun lagMottaker(rolle: Mottakerroller) = Mottaker.medRolle(rolle).apply {
        when (rolle) {
            Mottakerroller.BRUKER -> aktørId = FNR
            Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> institusjonID = INSITUSJON_ID
            Mottakerroller.FULLMEKTIG -> throw IllegalArgumentException("For FULLMEKTIG, bruk lagMottakerFullmektig()")
            else -> orgnr = ORGNR
        }
    }

    private fun lagMottakerFullmektig(mottakerType: Aktoertype) = Mottaker.medRolle(Mottakerroller.FULLMEKTIG).apply {
        when (mottakerType) {
            Aktoertype.PERSON -> personIdent = REP_FNR
            Aktoertype.ORGANISASJON -> orgnr = REP_ORGNR
            else -> throw IllegalArgumentException("Mottakertype må være person eller organisasjon")
        }
    }

    private fun lagMetadataMedArbeidsgiver() = DokumentbestillingMetadata().apply {
        mottaker = lagMottaker(Mottakerroller.ARBEIDSGIVER)
        mottakerID = ORGNR
        dokumenttypeID = "dok_1234"
    }

    private fun lagMetadataMedFullmektig(fullmektigType: Aktoertype) = DokumentbestillingMetadata().apply {
        mottaker = lagMottakerFullmektig(fullmektigType)
        mottakerID = when (fullmektigType) {
            Aktoertype.PERSON -> REP_FNR
            Aktoertype.ORGANISASJON -> REP_ORGNR
            else -> throw IllegalArgumentException("Mottakertype må være person eller organisasjon")
        }
        dokumenttypeID = "dok_1234"
    }

    private fun lagMetadataMedUtenlandskMyndighet() = DokumentbestillingMetadata().apply {
        mottaker = lagMottaker(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
        mottakerID = ORGNR
        utenlandskMyndighet = lagUtenlandskMyndighet()
        dokumenttypeID = "dok_1234"
    }

    private fun lagUtenlandskMyndighet() = UtenlandskMyndighet().apply {
        gateadresse1 = "Stubenstrasse 77"
        postnummer = "0101"
        poststed = "Berlin"
        landkode = Land_iso2.GL
        institusjonskode = "INST-023%zdf"
    }

    private fun lagMetadataMedNorskMyndighet() = DokumentbestillingMetadata().apply {
        mottaker = lagMottaker(Mottakerroller.NORSK_MYNDIGHET)
        mottakerID = ORGNR
        dokumenttypeID = "dok_1234"
    }

    private fun lagBrevData(): Element = try {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
    } catch (e: ParserConfigurationException) {
        throw IllegalStateException(e)
    }.newDocument().createElement("brevData")
}
