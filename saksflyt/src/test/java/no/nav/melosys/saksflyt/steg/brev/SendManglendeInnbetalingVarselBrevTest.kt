package no.nav.melosys.saksflyt.steg.brev

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Fullmakt
import no.nav.melosys.domain.brev.TrygdeavgiftBetalingsfrist
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import kotlin.test.Test

class SendManglendeInnbetalingVarselBrevTest {

    private lateinit var dokumentServiceFasade: DokumentServiceFasade
    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService
    private lateinit var sendManglendeInnbetalingVarselBrev: SendManglendeInnbetalingVarselBrev

    @BeforeEach
    fun setUp() {
        dokumentServiceFasade = mockk(relaxed = true)
        trygdeavgiftsberegningService = mockk(relaxed = true)
        sendManglendeInnbetalingVarselBrev = SendManglendeInnbetalingVarselBrev(dokumentServiceFasade, trygdeavgiftsberegningService)
    }

    @Test
    fun `inngangsSteg skal returnere SEND_MANGLENDE_INNBETALING_VARSELBREV`() {
        val steg = sendManglendeInnbetalingVarselBrev.inngangsSteg()
        assertEquals(ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV, steg)
    }

    @Test
    fun `utfør skal produsere dokument med korrekt data`() {
        val prosessinstans = Prosessinstans()
        val behandling = Behandling().apply {
            id = 123
            fagsak = Fagsak()
        }
        prosessinstans.behandling = behandling
        prosessinstans.setData(ProsessDataKey.BETALINGSSTATUS, Betalingsstatus.DELVIS_BETALT)
        prosessinstans.setData(ProsessDataKey.FAKTURANUMMER, "Fakturanummer")

        val capturedBrevbestillingDto = slot<BrevbestillingDto>()


        sendManglendeInnbetalingVarselBrev.utfør(prosessinstans)


        verify { dokumentServiceFasade.produserDokument(123, capture(capturedBrevbestillingDto)) }

        capturedBrevbestillingDto.captured.run {
            produserbardokument shouldBe Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING
            mottaker shouldBe Mottakerroller.BRUKER
            betalingsfrist shouldBe TrygdeavgiftBetalingsfrist.beregnTrygdeavgiftBetalingsfrist(LocalDate.now())
            fakturanummer shouldBe "Fakturanummer"
            betalingsstatus shouldBe Betalingsstatus.DELVIS_BETALT
            fullmektigForBetaling shouldBe null
        }
    }

    @Test
    fun `utfør skal produsere dokument med riktig fullmektigForBetaling`() {
        val prosessinstans = Prosessinstans()
        val behandling = Behandling().apply {
            id = 123
            fagsak = Fagsak().apply {
                aktører = mutableSetOf(Aktoer().apply {
                    aktørId = "123"
                    rolle = Aktoersroller.FULLMEKTIG
                    fullmakter = mutableSetOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                })
            }
        }
        prosessinstans.behandling = behandling
        prosessinstans.setData(ProsessDataKey.BETALINGSSTATUS, Betalingsstatus.DELVIS_BETALT)
        every { (trygdeavgiftsberegningService.finnFakturamottakerNavn(123)) } returns "Isa Testesen"

        val capturedBrevbestillingDto = slot<BrevbestillingDto>()


        sendManglendeInnbetalingVarselBrev.utfør(prosessinstans)


        verify { dokumentServiceFasade.produserDokument(123, capture(capturedBrevbestillingDto)) }

        capturedBrevbestillingDto.captured.run {
            fullmektigForBetaling shouldBe "Isa Testesen"
        }
    }

}
