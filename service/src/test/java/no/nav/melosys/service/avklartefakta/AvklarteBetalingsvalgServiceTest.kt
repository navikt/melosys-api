package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Betalingstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarteBetalingsvalgServiceTest {
    @MockK(relaxed = true)
    private lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var service: AvklarteBetalingsvalgService

    @BeforeEach
    fun setUp() {
        service = AvklarteBetalingsvalgService(avklartefaktaService)
    }

    @Test
    fun `hentAvklarteBetalingsvalg returnerer null når avklart fakta ikke finnes`() {
        val behandlingId = 1L
        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns emptySet()

        service.hentAvklarteBetalingsvalg(behandlingId).shouldBeNull()
    }

    @Test
    fun `Lagre og hent betalingsvalg som FAKTURA`() {
        val behandlingId = 1L
        val avklartFaktaDto = AvklartefaktaDto(listOf("FAKTURA"),Avklartefaktatyper.BETALINGSVALG.kode).apply {
            avklartefaktaType = Avklartefaktatyper.BETALINGSVALG
        }

        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns setOf(avklartFaktaDto)

        service.lagreBetalingsvalgSomAvklartefakta(behandlingId, Betalingstype.FAKTURA)
        service.hentAvklarteBetalingsvalg(behandlingId)?.equals(Betalingstype.FAKTURA)

        verify {
            avklartefaktaService.slettAvklarteFakta(behandlingId, Avklartefaktatyper.BETALINGSVALG)
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingId,
                Avklartefaktatyper.BETALINGSVALG,
                Avklartefaktatyper.BETALINGSVALG.kode,
                null,
                "FAKTURA"
            )
        }
    }

    @Test
    fun `Lagre og hent betalingsvalg som TREKK`() {
        val behandlingId = 1L
        val avklartFaktaDto = AvklartefaktaDto(listOf("TREKK"),Avklartefaktatyper.BETALINGSVALG.kode).apply {
            avklartefaktaType = Avklartefaktatyper.BETALINGSVALG
        }

        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns setOf(avklartFaktaDto)

        service.lagreBetalingsvalgSomAvklartefakta(behandlingId, Betalingstype.TREKK)
        service.hentAvklarteBetalingsvalg(behandlingId)?.equals(Betalingstype.TREKK)

        verify {
            avklartefaktaService.slettAvklarteFakta(behandlingId, Avklartefaktatyper.BETALINGSVALG)
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingId,
                Avklartefaktatyper.BETALINGSVALG,
                Avklartefaktatyper.BETALINGSVALG.kode,
                null,
                "TREKK"
            )
        }
    }
}
