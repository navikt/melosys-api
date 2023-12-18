package no.nav.melosys.saksflyt

import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.ProsessinstansInfo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*
import java.util.Set

@ExtendWith(MockitoExtension::class) // TODO: bruk mockk
internal class ProsessinstansBehandlerDelegateTest {
    @Mock
    private val prosessinstansRepository: ProsessinstansRepository? = null

    @Mock
    private lateinit var prosessinstansBehandler: ProsessinstansBehandler

    private lateinit var prosessinstansBehandlerDelegate: ProsessinstansBehandlerDelegate

    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setup() {
        prosessinstansBehandlerDelegate =
            ProsessinstansBehandlerDelegate(prosessinstansBehandler, prosessinstansRepository!!)
        prosessinstans = Prosessinstans()
        prosessinstans.id = UUID.randomUUID()
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_harIkkeLås_settesIkkePåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)
        Assertions.assertThat(prosessinstans.status).isEqualTo(ProsessStatus.KLAR)
        Mockito.verifyNoInteractions(prosessinstansRepository, prosessinstansBehandler)
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUnderBehandling_settesIkkePåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "12_12_1"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = prosessinstans(låsReferanse)
        Mockito.`when`(
            prosessinstansRepository!!.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
                ArgumentMatchers.eq(
                    prosessinstans.id
                ), ArgumentMatchers.any(), ArgumentMatchers.any()
            )
        )
            .thenReturn(Set.of(ProsessinstansInfo(eksisterendeProsessinstans)))

        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)
        Assertions.assertThat(prosessinstans.status).isEqualTo(ProsessStatus.KLAR)
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUlikId_settesPåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "12_12_1"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = prosessinstans("12_13_1")
        Mockito.`when`(
            prosessinstansRepository!!.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(
                ArgumentMatchers.eq(
                    prosessinstans.id
                ), ArgumentMatchers.any(), ArgumentMatchers.any()
            )
        )
            .thenReturn(Set.of(ProsessinstansInfo(eksisterendeProsessinstans)))

        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)
        Assertions.assertThat(prosessinstans.status).isEqualTo(ProsessStatus.PÅ_VENT)
        Mockito.verify(prosessinstansRepository).save(prosessinstans)
    }

    private fun prosessinstans(låsReferanse: String): Prosessinstans {
        val prosessinstans = Prosessinstans()
        prosessinstans.id = UUID.randomUUID()
        prosessinstans.låsReferanse = låsReferanse
        prosessinstans.status = ProsessStatus.UNDER_BEHANDLING
        prosessinstans.registrertDato = LocalDateTime.now()
        prosessinstans.registrertDato = LocalDateTime.now()
        return prosessinstans
    }
}
