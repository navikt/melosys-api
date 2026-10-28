package no.nav.melosys.saksflyt.steg.brev

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.oppgave.ÅrsakIkkeSendtInnhentingsbrev
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import kotlin.test.Test

class SendInnhentingAvInntektsopplysningerBrevTest {

    private val dokumentServiceFasade: DokumentServiceFasade = mockk(relaxed = true)
    private val brevmottakerService: BrevmottakerService = mockk()
    private val persondataFasade: PersondataFasade = mockk()
    private val eregFasade: EregFasade = mockk()
    private val oppgaveService: OppgaveService = mockk(relaxed = true)
    private val persondata: Persondata = mockk()

    private val sendInnhentingAvInntektsopplysningerBrev = SendInnhentingAvInntektsopplysningerBrev(
        dokumentServiceFasade, brevmottakerService, persondataFasade, eregFasade, oppgaveService
    )

    private val prosessinstans = Prosessinstans.forTest {
        behandling { id = BEHANDLING_ID }
        medData(ProsessDataKey.SEND_INNHENTINGSBREV, true)
    }

    @BeforeEach
    fun setUp() {
        every { brevmottakerService.avklarMottaker(any(), any(), any()) } returns
            Mottaker(rolle = Mottakerroller.BRUKER, aktørId = AKTØR_ID)
        every { persondataFasade.hentPerson(AKTØR_ID) } returns persondata
        every { persondata.erPersonDød() } returns false
        every { persondata.manglerGyldigRegistrertAdresse() } returns false
    }

    @Test
    fun `inngangsSteg skal returnere SEND_INNHENTINGSBREV_AARSAVREGNING`() {
        assertEquals(ProsessSteg.SEND_INNHENTINGSBREV_AARSAVREGNING, sendInnhentingAvInntektsopplysningerBrev.inngangsSteg())
    }

    @Test
    fun `utfør skal sende innhentingsbrev til bruker når flagget er satt`() {
        val capturedDto = slot<BrevbestillingDto>()
        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 1) { dokumentServiceFasade.produserDokument(BEHANDLING_ID, capture(capturedDto)) }
        capturedDto.captured.produserbardokument shouldBe Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER
        capturedDto.captured.mottaker shouldBe Mottakerroller.BRUKER
        verify(exactly = 0) { oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende brev når flagget er false (saksbehandlingsflyt-kontekst)`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling { id = BEHANDLING_ID }
            medData(ProsessDataKey.SEND_INNHENTINGSBREV, false)
        }

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende brev når flagget mangler`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling { id = BEHANDLING_ID }
        }

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende brev men oppdatere oppgave når mottaker er død og mangler adresse`() {
        every { persondata.erPersonDød() } returns true
        every { persondata.manglerGyldigRegistrertAdresse() } returns true

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
        verify(exactly = 1) {
            oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(BEHANDLING_ID, ÅrsakIkkeSendtInnhentingsbrev.PERSON_DØD_UTEN_ADRESSE)
        }
    }

    @Test
    fun `utfør skal sende brev når mottaker er død men har adresse`() {
        every { persondata.erPersonDød() } returns true

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 1) { dokumentServiceFasade.produserDokument(BEHANDLING_ID, any()) }
        verify(exactly = 0) { oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende brev men oppdatere oppgave når levende mottaker mangler adresse`() {
        every { persondata.manglerGyldigRegistrertAdresse() } returns true

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
        verify(exactly = 1) {
            oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(BEHANDLING_ID, ÅrsakIkkeSendtInnhentingsbrev.PERSON_UTEN_ADRESSE)
        }
    }

    @Test
    fun `utfør skal ikke sende brev men oppdatere oppgave når fullmektig virksomhet er slettet`() {
        mottakerErFullmektigVirksomhet(opphoersdato = LocalDate.now().minusYears(1))

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
        verify(exactly = 1) {
            oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(BEHANDLING_ID, ÅrsakIkkeSendtInnhentingsbrev.VIRKSOMHET_SLETTET)
        }
    }

    @Test
    fun `utfør skal sende brev når fullmektig virksomhet ikke er slettet`() {
        mottakerErFullmektigVirksomhet(opphoersdato = null)

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 1) { dokumentServiceFasade.produserDokument(BEHANDLING_ID, any()) }
        verify(exactly = 0) { oppgaveService.oppdaterOppgaveVedIkkeSendtInnhentingsbrev(any(), any()) }
    }

    @Test
    fun `utfør skal sende brev når fullmektig virksomhet har opphørsdato i dag`() {
        mottakerErFullmektigVirksomhet(opphoersdato = LocalDate.now())

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 1) { dokumentServiceFasade.produserDokument(BEHANDLING_ID, any()) }
    }

    private fun mottakerErFullmektigVirksomhet(opphoersdato: LocalDate?) {
        every { brevmottakerService.avklarMottaker(any(), any(), any()) } returns
            Mottaker(rolle = Mottakerroller.FULLMEKTIG, orgnr = ORGNR)
        every { eregFasade.hentOrganisasjon(ORGNR) } returns Saksopplysning().apply {
            dokument = OrganisasjonDokumentTestFactory.builder()
                .orgnummer(ORGNR)
                .organisasjonsDetaljer(OrganisasjonsDetaljer(orgnummer = ORGNR, opphoersdato = opphoersdato))
                .build()
        }
    }

    companion object {
        private const val BEHANDLING_ID = 123L
        private const val AKTØR_ID = "1234567890123"
        private const val ORGNR = "980515877"
    }
}
