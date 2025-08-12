package no.nav.melosys.service.altinn

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.JAXBException
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.msm.AltinnDokument
import no.nav.melosys.integrasjon.soknadmottak.SoknadMottakConsumer
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM
import no.nav.melosys.soknad_altinn.ObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
class AltinnSoeknadServiceKtTest {

    @RelaxedMockK
    lateinit var soknadMottakConsumer: SoknadMottakConsumer

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    private lateinit var altinnSoeknadService: AltinnSoeknadService

    @BeforeEach
    fun setup() {
        altinnSoeknadService = AltinnSoeknadService(
            soknadMottakConsumer,
            fagsakService,
            mottatteOpplysningerService,
            persondataFasade,
            avklarteVirksomheterService
        )
    }

    @Test
    fun `opprett fagsak og behandling fra altinn søknad søknad eksisterer verifiser fagsak behandling og mottatte opplysninger opprettet`() {
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak
        every { persondataFasade.hentAktørIdForIdent(any()) } returns aktørID


        val result = altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        result shouldBe fagsak.finnAktivBehandlingIkkeÅrsavregning()
        verify { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) }
        verify { mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(eq(1L), any(), any(), eq(søknadID)) }

        opprettSakRequestSlot.captured.run {
            sakstype shouldBe Sakstyper.EU_EOS
            behandlingstema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER
            behandlingstype shouldBe Behandlingstyper.FØRSTEGANG
            behandlingsårsaktype shouldBe Behandlingsaarsaktyper.SØKNAD
            mottaksdato shouldBe LocalDate.ofInstant(søknadDokument.innsendtTidspunkt, ZoneId.systemDefault())
            arbeidsgiver shouldBe søknad.innhold.arbeidsgiver.virksomhetsnummer
            aktørID shouldBe aktørID
        }
    }

    @Test
    fun `opprett fagsak og behandling fra altinn søknad søknad eksisterer arbeidsgiver offentlig verifiser arbeid tjenesteperson eller fly`() {
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        søknad.innhold.arbeidsgiver.setOffentligVirksomhet(true)
        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak
        every { persondataFasade.hentAktørIdForIdent(any()) } returns aktørID


        val result = altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        result shouldBe fagsak.finnAktivBehandlingIkkeÅrsavregning()
        verify { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) }

        opprettSakRequestSlot.captured.run {
            behandlingstema shouldBe Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            behandlingstype shouldBe Behandlingstyper.FØRSTEGANG
            arbeidsgiver shouldBe søknad.innhold.arbeidsgiver.virksomhetsnummer
            aktørID shouldBe aktørID
        }
    }

    @Test
    fun `opprett sak fra altinn søknad rådgivningsfirma er fullmektig lager fullmektig`() {
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak
        every { persondataFasade.hentAktørIdForIdent(any()) } returns aktørID


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        verify { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) }

        val fullmektigVirksomhetsnummer = søknad.innhold.fullmakt.fullmektigVirksomhetsnummer
        opprettSakRequestSlot.captured.fullmektig?.run {
            orgnr shouldBe fullmektigVirksomhetsnummer
            fullmakter shouldContainExactly listOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
        }
    }

    @Test
    fun `opprett sak fra altinn søknad fullmakt uten rådgivningsfirma lager arbeidsgiver som fullmektig`() {
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        søknad.innhold.fullmakt.fullmektigVirksomhetsnummer = null
        søknad.innhold.fullmakt.setFullmaktFraArbeidstaker(true)
        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak
        every { persondataFasade.hentAktørIdForIdent(any()) } returns aktørID


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        verify { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) }

        val fullmektigVirksomhetsnummer = søknad.innhold.arbeidsgiver.virksomhetsnummer
        opprettSakRequestSlot.captured.fullmektig?.run {
            orgnr shouldBe fullmektigVirksomhetsnummer
            fullmakter shouldContainExactly listOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
        }
    }

    @Test
    fun `opprett sak fra altinn søknad kontaktperson navn finnes lager kontaktopplysninger`() {
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        søknad.innhold.arbeidsgiver.kontaktperson.kontaktpersonNavn = "Ola"
        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak
        every { persondataFasade.hentAktørIdForIdent(any()) } returns aktørID


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        verify { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) }

        opprettSakRequestSlot.captured.run {
            kontaktopplysninger.shouldNotBeEmpty()
            kontaktopplysninger.first().kontaktNavn shouldBe søknad.innhold.arbeidsgiver.kontaktperson.kontaktpersonNavn
        }
    }

    @Test
    fun `opprett sak fra altinn søknad arbeidstaker har utenlandsk ID nummer utenlandsk person ID blir satt`() {
        val utenlandskPersonId = "utenlandskPersonId"
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        søknad.innhold.arbeidstaker.utenlandskIDnummer = utenlandskPersonId
        val opprettSakRequestSlot = slot<OpprettSakRequest>()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(capture(opprettSakRequestSlot)) } returns fagsak


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        verify { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) }
        opprettSakRequestSlot.captured.utenlandskPersonId shouldBe utenlandskPersonId
    }

    @Test
    fun `opprett fagsak og behandling fra altinn søknad virksomhet lagres som avklart fakta`() {
        val fagsak = lagFagsak()
        val søknad = lagMedlemskapArbeidEOSM()
        every { soknadMottakConsumer.hentSøknad(søknadID) } returns søknad
        every { soknadMottakConsumer.hentDokumenter(søknadID) } returns setOf(søknadDokument)
        every { fagsakService.nyFagsakOgBehandling(any<OpprettSakRequest>()) } returns fagsak


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)


        verify {
            avklarteVirksomheterService.lagreVirksomhetSomAvklartfakta(
                søknad.innhold.arbeidsgiver.virksomhetsnummer,
                fagsak.finnAktivBehandlingIkkeÅrsavregning()?.id
            )
        }
    }

    private fun lagMedlemskapArbeidEOSM(): MedlemskapArbeidEOSM {
        val jaxbContext = JAXBContext.newInstance(ObjectFactory::class.java)
        val url = javaClass.classLoader.getResource("altinn/NAV_MedlemskapArbeidEOS.xml")
        return try {
            (jaxbContext.createUnmarshaller().unmarshal(url) as JAXBElement<MedlemskapArbeidEOSM>).value
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }
    }

    private fun lagFagsak() = Fagsak.forTest {
        behandlinger(Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.OPPRETTET
        })
    }

    companion object {
        private const val søknadID = "13423"
        private const val aktørID = "123321123"
        private val søknadDokument = AltinnDokument(
            søknadID,
            "dokID123",
            "tittel",
            AltinnDokument.AltinnDokumentType.SOKNAD.name,
            "Base64EncodedPdf",
            Instant.EPOCH
        )
    }
}
