package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalposttype
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.skjema.types.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class OpprettOgFerdigstillJournalpostSøknadTest {

    @MockK
    lateinit var melosysSkjemaApiClient: MelosysSkjemaApiClient

    @MockK
    lateinit var joarkFasade: JoarkFasade

    @MockK
    lateinit var behandlingService: BehandlingService

    private lateinit var opprettOgFerdigstillJournalpostSøknad: OpprettOgFerdigstillJournalpostSøknad

    private val fnr = "12345678901"
    private val orgnr = "123456789"
    private val juridiskEnhetOrgnr = "987654321"
    private val referanseId = "MEL-TEST123"
    private val saksnummer = "SAK-12345"
    private val journalpostId = "JOARK-123456"
    private val skjemaId = UUID.randomUUID()
    private val pdfBytes = "PDF content".toByteArray()

    private val capturedJournalpost = slot<OpprettJournalpost>()

    @BeforeEach
    fun setup() {
        opprettOgFerdigstillJournalpostSøknad = OpprettOgFerdigstillJournalpostSøknad(
            melosysSkjemaApiClient, joarkFasade, behandlingService
        )

        every { melosysSkjemaApiClient.hentPdf(skjemaId) } returns pdfBytes
        every { joarkFasade.opprettJournalpost(capture(capturedJournalpost), eq(true)) } returns journalpostId
        every { behandlingService.lagre(any()) } just Runs
    }

    @Test
    fun `inngangsSteg returnerer OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD`() {
        opprettOgFerdigstillJournalpostSøknad.inngangsSteg() shouldBe ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD
    }

    @Test
    fun `utfør oppretter journalpost med korrekte verdier for arbeidstaker-del`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostSøknad.utfør(prosessinstans)

        verify { melosysSkjemaApiClient.hentPdf(skjemaId) }
        verify { joarkFasade.opprettJournalpost(any(), eq(true)) }
        verify { behandlingService.lagre(prosessinstans.behandling!!) }

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
    }

    @Test
    fun `utfør oppretter journalpost med korrekt tittel for arbeidsgiver-del`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSGIVERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostSøknad.utfør(prosessinstans)

        val opprettJournalpost = capturedJournalpost.captured
        opprettJournalpost.innhold shouldBe "Bekreftelse på utsending i EØS eller Sveits"
        opprettJournalpost.hoveddokument.tittel shouldBe "Bekreftelse på utsending i EØS eller Sveits"
    }

    @Test
    fun `utfør setter journalpostId på behandling`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)
        val behandling = prosessinstans.behandling!!

        opprettOgFerdigstillJournalpostSøknad.utfør(prosessinstans)

        behandling.initierendeJournalpostId shouldBe journalpostId
    }

    @Test
    fun `utfør kaller joarkFasade med forsøkEndeligJfr true`() {
        val søknadsdata = lagSøknadsdata(Skjemadel.ARBEIDSTAKERS_DEL)
        val prosessinstans = lagProsessinstans(søknadsdata)

        opprettOgFerdigstillJournalpostSøknad.utfør(prosessinstans)

        verify { joarkFasade.opprettJournalpost(any(), eq(true)) }
    }

    private fun lagSøknadsdata(skjemadel: Skjemadel): UtsendtArbeidstakerM2MSkjemaData {
        val metadata = when (skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL -> DegSelvMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )
            Skjemadel.ARBEIDSGIVERS_DEL -> ArbeidsgiverMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhetOrgnr
            )
        }

        val data = when (skjemadel) {
            Skjemadel.ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            Skjemadel.ARBEIDSGIVERS_DEL -> UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }

        return UtsendtArbeidstakerM2MSkjemaData(
            skjemaer = listOf(
                UtsendtArbeidstakerSkjemaDto(
                    id = skjemaId,
                    status = SkjemaStatus.SENDT,
                    fnr = fnr,
                    orgnr = orgnr,
                    metadata = metadata,
                    data = data
                )
            ),
            referanseId = referanseId
        )
    }

    private fun lagProsessinstans(søknadsdata: UtsendtArbeidstakerM2MSkjemaData): Prosessinstans {
        val fagsak = mockk<Fagsak>()
        val behandling = mockk<Behandling>(relaxed = true)

        every { fagsak.saksnummer } returns saksnummer
        every { behandling.fagsak } returns fagsak

        return Prosessinstans.forTest {
            medData(ProsessDataKey.SØKNADSDATA, søknadsdata)
            this.behandling = behandling
        }
    }
}
