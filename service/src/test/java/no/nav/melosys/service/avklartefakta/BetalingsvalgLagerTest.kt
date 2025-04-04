package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Betalingstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockKExtension::class)
class BetalingsvalgLagerTest {
    @MockK(relaxed = true)
    private lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var service: BetalingsvalgLager

    @BeforeEach
    fun setUp() {
        service = BetalingsvalgLager(avklartefaktaService)
    }

    @Test
    fun `hentAvklarteBetalingsvalg returnerer null når avklart fakta ikke finnes`() {
        val behandlingId = 1L
        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns emptySet()

        service.hentAvklarteBetalingsvalg(behandlingId).shouldBeNull()
    }

    @ParameterizedTest
    @EnumSource(Betalingstype::class)
    fun `Lagre betalingsvalg`(betalingstype: Betalingstype) {
        val behandlingId = 1L


        service.lagreBetalingsvalgSomAvklartefakta(behandlingId, betalingstype)


        verify {
            avklartefaktaService.slettAvklarteFakta(behandlingId, Avklartefaktatyper.BETALINGSVALG)
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingId,
                Avklartefaktatyper.BETALINGSVALG,
                Avklartefaktatyper.BETALINGSVALG.kode,
                null,
                betalingstype.kode
            )
        }
    }

    @ParameterizedTest
    @EnumSource(Betalingstype::class)
    fun `Hent betalingsvalg`(betalingstype: Betalingstype){
        val behandlingId = 1L
        val avklartFaktaDto = AvklartefaktaDto(listOf(betalingstype.kode),Avklartefaktatyper.BETALINGSVALG.kode).apply {
            avklartefaktaType = Avklartefaktatyper.BETALINGSVALG
        }

        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns setOf(avklartFaktaDto)

        service.lagreBetalingsvalgSomAvklartefakta(behandlingId, betalingstype)
        val result = service.hentAvklarteBetalingsvalg(behandlingId)

        result.shouldBe(betalingstype)
    }

}
