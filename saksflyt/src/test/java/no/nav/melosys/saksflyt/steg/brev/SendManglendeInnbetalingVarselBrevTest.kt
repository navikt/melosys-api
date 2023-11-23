package no.nav.melosys.saksflyt.steg.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import io.mockk.verify
import kotlin.test.Test

class SendManglendeInnbetalingVarselBrevTest {

    private lateinit var dokumentServiceFasade: DokumentServiceFasade
    private lateinit var fagsakService: FagsakService
    private lateinit var sendManglendeInnbetalingVarselBrev: SendManglendeInnbetalingVarselBrev

    @BeforeEach
    fun setUp() {
        dokumentServiceFasade = mockk(relaxed = true)
        fagsakService = mockk(relaxed = true)
        sendManglendeInnbetalingVarselBrev = SendManglendeInnbetalingVarselBrev(dokumentServiceFasade, fagsakService)
    }

    @Test
    fun `inngangsSteg skal returnere SEND_MANGLENDE_INNBETALING_VARSELBREV`() {
        val steg = sendManglendeInnbetalingVarselBrev.inngangsSteg()
        assertEquals(ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV, steg)
    }

    @Test
    fun `utfør skal produsere dokument med korrekt data`() {
        val prosessinstans = Prosessinstans()
        val behandling = Behandling().apply { id = 123 }
        val fagsak = Fagsak().apply {
            saksnummer = "Saksnummer"
            behandlinger = mutableListOf(behandling)
        }

        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "Saksnummer")
        prosessinstans.setData(ProsessDataKey.BETALINGSSTATUS, Betalingsstatus.DELVIS_BETALT.name)
        prosessinstans.setData(ProsessDataKey.FAKTURANUMMER, "Fakturanummer")

        every { fagsakService.hentFagsak("Saksnummer") } returns fagsak

        sendManglendeInnbetalingVarselBrev.utfør(prosessinstans)

        verify { dokumentServiceFasade.produserDokument(123, any<BrevbestillingDto>()) }
    }

}
