package no.nav.melosys.saksflyt.steg.jfr

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class OppdaterOgFerdigstillJournalpostTest {

    @RelaxedMockK
    private lateinit var joarkFasade: JoarkFasade
    private val oppgaveFactory = OppgaveFactory()
    private var journalpostOppdateringSlot = slot<JournalpostOppdatering>()

    private lateinit var oppdaterOgFerdigstillJournalpost: OppdaterOgFerdigstillJournalpost

    @BeforeEach
    fun setUp() {
        oppdaterOgFerdigstillJournalpost = OppdaterOgFerdigstillJournalpost(joarkFasade, oppgaveFactory)
        journalpostOppdateringSlot.clear()
    }

    @Test
    fun utfør_alleFeltSatt_sendesKorrekt() {
        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.BRUKER_ID, BRUKER_ID)
            setData(ProsessDataKey.VIRKSOMHET_ORGNR, VIRKSOMHET_ORGNR)
            setData(ProsessDataKey.DOKUMENT_ID, DOKUMENT_ID)
            setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, HOVEDDOKUMENT_TITTEL)
            setData(ProsessDataKey.MOTTATT_DATO, MOTTATT_DATO)
            setData(ProsessDataKey.AVSENDER_NAVN, AVSENDER_NAVN)
            setData(ProsessDataKey.AVSENDER_ID, AVSENDER_ID)
            setData(ProsessDataKey.AVSENDER_LAND, AVSENDER_LAND)
            setData(ProsessDataKey.AVSENDER_TYPE, AVSENDER_TYPE)
            setData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, LOGISKE_VEDLEGG_TITLER)
            setData(ProsessDataKey.FYSISKE_VEDLEGG, FYSISKE_VEDLEGG)
        }


        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans)


        verify { joarkFasade.oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, capture(journalpostOppdateringSlot)) }
        journalpostOppdateringSlot.captured.shouldNotBeNull().run {
            saksnummer.shouldBe(FagsakTestFactory.SAKSNUMMER)
            brukerID.shouldBe(BRUKER_ID)
            virksomhetOrgnr.shouldBe(VIRKSOMHET_ORGNR)
            hovedDokumentID.shouldBe(DOKUMENT_ID)
            tittel.shouldBe(HOVEDDOKUMENT_TITTEL)
            mottattDato.shouldBe(MOTTATT_DATO)
            tema.shouldBe("MED")
            avsenderID.shouldBe(AVSENDER_ID)
            avsenderNavn.shouldBe(AVSENDER_NAVN)
            avsenderType.shouldBe(AVSENDER_TYPE)
            avsenderLand.shouldBe(AVSENDER_LAND)
            fysiskeVedlegg.shouldBe(FYSISKE_VEDLEGG)
            logiskeVedleggTitler.shouldBe(LOGISKE_VEDLEGG_TITLER)
        }
    }

    @Test
    fun utfør_avsenderNavnErNull_setterAvsenderNavnTilAvsenderId() {
        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.AVSENDER_ID, AVSENDER_ID)
        }


        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans)


        verify { joarkFasade.oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, capture(journalpostOppdateringSlot)) }
        journalpostOppdateringSlot.captured.avsenderNavn.shouldBe(AVSENDER_ID)
    }

    @Test
    fun utfør_avsenderNavnErSatt_brukerAvsenderNavn() {
        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.AVSENDER_NAVN, AVSENDER_NAVN)
        }


        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans)


        verify { joarkFasade.oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, capture(journalpostOppdateringSlot)) }
        journalpostOppdateringSlot.captured.avsenderNavn.shouldBe(AVSENDER_NAVN)
    }

    @Test
    fun utfør_avsenderIdOgNavnErIkkeSatt_kasterFeil() {
        val prosessinstans = lagProsessinstans()


        shouldThrow<FunksjonellException> { oppdaterOgFerdigstillJournalpost.utfør(prosessinstans) }
            .message.shouldBe("Både avsenderID og AvsenderNavn er null. AvsenderNavn er påkrevd for å journalføre.")
    }

    @Test
    fun utfør_mottakerKanalErEessi_setterIkkeAvsender() {
        val prosessinstans = lagProsessinstans().apply {
            setData(ProsessDataKey.MOTTAKSKANAL_ER_ELEKTRONISK, true)
        }


        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans)


        verify { joarkFasade.oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, capture(journalpostOppdateringSlot)) }
        journalpostOppdateringSlot.captured.shouldNotBeNull().run {
            avsenderID.shouldBeNull()
            avsenderNavn.shouldBeNull()
            avsenderLand.shouldBeNull()
            avsenderType.shouldBeNull()
        }
    }


    private fun lagProsessinstans() = Prosessinstans().apply {
        type = ProsessType.JFR_NY_SAK_BRUKER
        setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID)
        behandling = Behandling().apply {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak = FagsakTestFactory.lagFagsak()
            type = Behandlingstyper.FØRSTEGANG
        }
    }


    private val JOURNALPOST_ID = "123"
    private val BRUKER_ID = "231"
    private val VIRKSOMHET_ORGNR = "456"
    private val DOKUMENT_ID = "4424224"
    private val HOVEDDOKUMENT_TITTEL = "Tittelei"
    private val MOTTATT_DATO = LocalDate.now().minusYears(1)
    private val AVSENDER_ID = "avsenderID"
    private val AVSENDER_NAVN = "avsendernavn"
    private val AVSENDER_TYPE = Avsendertyper.ORGANISASJON
    private val AVSENDER_LAND = "SE"
    private val FYSISKE_VEDLEGG = mapOf("id" to "doktittel")
    private val LOGISKE_VEDLEGG_TITLER = listOf("tittelen", "tittelto")
}

