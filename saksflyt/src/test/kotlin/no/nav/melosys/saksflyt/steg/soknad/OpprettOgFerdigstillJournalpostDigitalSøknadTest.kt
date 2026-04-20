package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalposttype
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpprettOgFerdigstillJournalpostDigitalSøknadTest {

    @MockK
    lateinit var melosysSkjemaApiClient: MelosysSkjemaApiClient

    @MockK
    lateinit var joarkFasade: JoarkFasade

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    @MockK
    lateinit var skjemaSakMappingService: no.nav.melosys.service.sak.SkjemaSakMappingService

    private lateinit var opprettOgFerdigstillJournalpostDigitalSøknad: OpprettOgFerdigstillJournalpostDigitalSøknad

    private val fnr = "12345678901"
    private val innsenderFnr = "98765432100"
    private val referanseId = "MEL-TEST123"
    private val saksnummer = "SAK-12345"
    private val journalpostId = "JOARK-123456"
    private val pdfBytes = "PDF content".toByteArray()

    private val capturedJournalpost = slot<OpprettJournalpost>()

    private val brukerNavn = "Test Testesen"
    private val innsenderNavn = "Innsender Innsendersen"

    @BeforeEach
    fun setup() {
        opprettOgFerdigstillJournalpostDigitalSøknad = OpprettOgFerdigstillJournalpostDigitalSøknad(
            melosysSkjemaApiClient, joarkFasade, behandlingService, persondataFasade, skjemaSakMappingService
        )

        every { joarkFasade.opprettJournalpost(capture(capturedJournalpost), eq(true)) } returns journalpostId
        every { behandlingService.lagre(any()) } just Runs
        every { persondataFasade.hentSammensattNavn(innsenderFnr) } returns innsenderNavn
        every { skjemaSakMappingService.oppdaterJournalpostId(any(), any()) } just Runs
    }

    @Test
    fun `inngangsSteg returnerer OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD`() {
        opprettOgFerdigstillJournalpostDigitalSøknad.inngangsSteg() shouldBe ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD
    }

    @Test
    fun `utfør oppretter journalpost med korrekte verdier for arbeidstaker-del`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostDigitalSøknad.utfør(prosessinstans)

        verify { melosysSkjemaApiClient.hentPdf(søknadsdata.skjema.id) }
        verify { joarkFasade.opprettJournalpost(any(), eq(true)) }
        verify { behandlingService.lagre(any()) }

        val opprettJournalpost = capturedJournalpost.captured
        opprettJournalpost.tema shouldBe "MED"
        opprettJournalpost.mottaksKanal shouldBe "NAV_NO"
        opprettJournalpost.saksnummer shouldBe saksnummer
        opprettJournalpost.brukerId shouldBe fnr
        opprettJournalpost.brukerIdType shouldBe BrukerIdType.FOLKEREGISTERIDENT
        opprettJournalpost.eksternReferanseId shouldBe referanseId
        opprettJournalpost.journalposttype shouldBe Journalposttype.INN
        opprettJournalpost.innhold shouldBe "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
        opprettJournalpost.hoveddokument.tittel shouldBe "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
        opprettJournalpost.korrespondansepartId shouldBe innsenderFnr
        opprettJournalpost.korrespondansepartNavn shouldBe innsenderNavn
    }

    @Test
    fun `utfør oppretter journalpost med korrekt tittel for arbeidsgiver-del`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSGIVERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostDigitalSøknad.utfør(prosessinstans)

        val opprettJournalpost = capturedJournalpost.captured
        opprettJournalpost.innhold shouldBe "Bekreftelse på utsending i EØS eller Sveits"
        opprettJournalpost.hoveddokument.tittel shouldBe "Bekreftelse på utsending i EØS eller Sveits"
    }

    @Test
    fun `utfør setter journalpostId på behandling`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)
        val behandling = prosessinstans.behandling!!

        opprettOgFerdigstillJournalpostDigitalSøknad.utfør(prosessinstans)

        behandling.initierendeJournalpostId shouldBe journalpostId
    }

    @Test
    fun `utfør kaller joarkFasade med forsøkEndeligJfr true`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostDigitalSøknad.utfør(prosessinstans)

        verify { joarkFasade.opprettJournalpost(any(), eq(true)) }
    }

    private fun lagSøknadsdata(skjemadel: Skjemadel) =
        lagUtsendtArbeidstakerSkjemaM2MDto {
            this.skjemadel = skjemadel
            fnr = this@OpprettOgFerdigstillJournalpostDigitalSøknadTest.fnr
            referanseId = this@OpprettOgFerdigstillJournalpostDigitalSøknadTest.referanseId
            innsenderFnr = this@OpprettOgFerdigstillJournalpostDigitalSøknadTest.innsenderFnr
            if (skjemadel == Skjemadel.ARBEIDSGIVERS_DEL) {
                data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            }
        }.also {
            every { melosysSkjemaApiClient.hentPdf(it.skjema.id) } returns pdfBytes
        }

    @Test
    fun `utfør overskriver ikke initierendeJournalpostId når den allerede er satt`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val eksisterendeJournalpostId = "JOARK-EKSISTERENDE"
        val behandling = Behandling.forTest {
            fagsak { saksnummer = this@OpprettOgFerdigstillJournalpostDigitalSøknadTest.saksnummer }
            initierendeJournalpostId = eksisterendeJournalpostId
        }
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            this.behandling = behandling
        }

        opprettOgFerdigstillJournalpostDigitalSøknad.utfør(prosessinstans)

        behandling.initierendeJournalpostId shouldBe eksisterendeJournalpostId
        verify(exactly = 0) { behandlingService.lagre(any()) }
    }

    @Test
    fun `utfør kaller oppdaterJournalpostId på skjemaSakMappingService`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostDigitalSøknad.utfør(prosessinstans)

        verify { skjemaSakMappingService.oppdaterJournalpostId(søknadsdata.skjema.id, journalpostId) }
    }

    private fun lagProsessinstans(søknadsdata: no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto): Prosessinstans {
        val behandling = Behandling.forTest {
            fagsak { saksnummer = this@OpprettOgFerdigstillJournalpostDigitalSøknadTest.saksnummer }
        }

        return Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            this.behandling = behandling
        }
    }
}
