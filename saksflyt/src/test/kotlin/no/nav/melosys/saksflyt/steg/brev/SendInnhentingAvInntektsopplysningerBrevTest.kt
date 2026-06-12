package no.nav.melosys.saksflyt.steg.brev

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class SendInnhentingAvInntektsopplysningerBrevTest {

    private lateinit var dokumentServiceFasade: DokumentServiceFasade
    private lateinit var sendInnhentingAvInntektsopplysningerBrev: SendInnhentingAvInntektsopplysningerBrev

    @BeforeEach
    fun setUp() {
        dokumentServiceFasade = mockk(relaxed = true)
        sendInnhentingAvInntektsopplysningerBrev = SendInnhentingAvInntektsopplysningerBrev(dokumentServiceFasade)
    }

    @Test
    fun `inngangsSteg skal returnere SEND_INNHENTINGSBREV_AARSAVREGNING`() {
        assertEquals(ProsessSteg.SEND_INNHENTINGSBREV_AARSAVREGNING, sendInnhentingAvInntektsopplysningerBrev.inngangsSteg())
    }

    @Test
    fun `utfør skal sende innhentingsbrev til bruker når flagget er satt`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling { id = 123 }
            medData(ProsessDataKey.SEND_INNHENTINGSBREV, true)
        }

        val capturedDto = slot<BrevbestillingDto>()
        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 1) { dokumentServiceFasade.produserDokument(123, capture(capturedDto)) }
        capturedDto.captured.produserbardokument shouldBe Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER
        capturedDto.captured.mottaker shouldBe Mottakerroller.BRUKER
    }

    @Test
    fun `utfør skal ikke sende brev når flagget er false (saksbehandlingsflyt-kontekst)`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling { id = 123 }
            medData(ProsessDataKey.SEND_INNHENTINGSBREV, false)
        }

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
    }

    @Test
    fun `utfør skal ikke sende brev når flagget mangler`() {
        val prosessinstans = Prosessinstans.forTest {
            behandling { id = 123 }
        }

        sendInnhentingAvInntektsopplysningerBrev.utfør(prosessinstans)

        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
    }
}
