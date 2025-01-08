package no.nav.melosys.service.avklartefakta

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklartUkjentSluttdatoServiceTest {

    @MockK(relaxed = true)
    private lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var service: AvklartUkjentSluttdatoService

    @BeforeEach
    fun setUp() {
        service = AvklartUkjentSluttdatoService(avklartefaktaService)
    }

    @Test
    fun `hentUkjentSluttdato returnerer null når avklart fakta ikke finnes`() {
        val behandlingId = 1L
        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns emptySet()

        service.hentUkjentSluttdato(behandlingId).shouldBeNull()
    }

    @Test
    fun `hentUkjentSluttdato kaster IllegalArgumentException ved flere fakta verdier`() {
        val behandlingId = 1L
        val avklartFaktaDto = AvklartefaktaDto(listOf("true", "false"), Avklartefaktatyper.UKJENT_SLUTTDATO.kode).apply {
            avklartefaktaType = Avklartefaktatyper.UKJENT_SLUTTDATO
        }

        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns setOf(avklartFaktaDto)

        shouldThrow<IllegalArgumentException> {
            service.hentUkjentSluttdato(behandlingId)
        }
    }

    @Test
    fun `lagreOgHent ukjent sluttdato som true`() {
        val behandlingId = 1L
        val avklartFaktaDto = AvklartefaktaDto(listOf("true"), Avklartefaktatyper.UKJENT_SLUTTDATO.kode).apply {
            avklartefaktaType = Avklartefaktatyper.UKJENT_SLUTTDATO
        }

        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns setOf(avklartFaktaDto)

        service.lagreUkjentSluttdatoSomAvklartefakta(behandlingId, true)
        service.hentUkjentSluttdato(behandlingId)?.shouldBeTrue()

        verify {
            avklartefaktaService.slettAvklarteFakta(behandlingId, Avklartefaktatyper.UKJENT_SLUTTDATO)
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingId,
                Avklartefaktatyper.UKJENT_SLUTTDATO,
                Avklartefaktatyper.UKJENT_SLUTTDATO.kode,
                null,
                "true"
            )
        }
    }

    @Test
    fun `lagreOgHent ukjent sluttdato som false`() {
        val behandlingId = 1L
        val avklartFaktaDto = AvklartefaktaDto(listOf("false"), Avklartefaktatyper.UKJENT_SLUTTDATO.kode).apply {
            avklartefaktaType = Avklartefaktatyper.UKJENT_SLUTTDATO
        }

        every { avklartefaktaService.hentAlleAvklarteFakta(behandlingId) } returns setOf(avklartFaktaDto)

        service.lagreUkjentSluttdatoSomAvklartefakta(behandlingId, false)
        service.hentUkjentSluttdato(behandlingId)?.shouldBeFalse()

        verify {
            avklartefaktaService.slettAvklarteFakta(behandlingId, Avklartefaktatyper.UKJENT_SLUTTDATO)
            avklartefaktaService.leggTilAvklarteFakta(
                behandlingId,
                Avklartefaktatyper.UKJENT_SLUTTDATO,
                Avklartefaktatyper.UKJENT_SLUTTDATO.kode,
                null,
                "false"
            )
        }
    }
}
