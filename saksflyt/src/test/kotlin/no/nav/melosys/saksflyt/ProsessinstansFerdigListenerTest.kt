package no.nav.melosys.saksflyt

import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
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
internal class ProsessinstansFerdigListenerTest {
    @Mock
    private val prosessinstansRepository: ProsessinstansRepository? = null

    @Mock
    private  lateinit var prosessinstansBehandler: ProsessinstansBehandler

    private lateinit var prosessinstansFerdigListener: ProsessinstansFerdigListener

    private var ferdigProsessinstans: Prosessinstans? = null

    @BeforeEach
    fun setup() {
        prosessinstansFerdigListener =
            ProsessinstansFerdigListener(prosessinstansRepository!!, prosessinstansBehandler)
        ferdigProsessinstans = Prosessinstans()
        ferdigProsessinstans!!.id = UUID.randomUUID()
    }

    @Test
    fun prosessinstansFerdig_harIngenLås_gjørIngenting() {
        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))
        Mockito.verifyNoInteractions(prosessinstansRepository, prosessinstansBehandler)
    }

    @Test
    fun prosesssinstansFerdig_harLåsFinnesAktiveReferanser_gjørIngenting() {
        ferdigProsessinstans!!.låsReferanse = "12_12_1"
        Mockito.`when`(
            prosessinstansRepository!!.existsByStatusNotInAndLåsReferanse(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        ).thenReturn(true)

        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))
        Mockito.verify(prosessinstansRepository)
            .existsByStatusNotInAndLåsReferanse(ArgumentMatchers.any(), ArgumentMatchers.any())
        Mockito.verifyNoMoreInteractions(prosessinstansRepository, prosessinstansBehandler)
    }

    @Test
    fun prosessinstansFerdig_harLåsIngenAktiveReferanser_starterTidligstOpprettetProsessinstans() {
        ferdigProsessinstans!!.låsReferanse = "12_12_1"

        val prosessinstansUlikReferanse = prosessinstans(LocalDateTime.now().minusDays(2), "13_12_1")
        val tidligstOpprettetProsessinstans = prosessinstans(LocalDateTime.now().minusDays(1), "12_13_1")
        val senestOpprettetProsessinstans = prosessinstans(LocalDateTime.now(), "12_14_1")

        Mockito.`when`(prosessinstansRepository!!.findAllByStatus(ProsessStatus.PÅ_VENT)).thenReturn(
            Set.of(prosessinstansUlikReferanse, tidligstOpprettetProsessinstans, senestOpprettetProsessinstans)
        )

        prosessinstansFerdigListener!!.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))
        Mockito.verify(prosessinstansBehandler).behandleProsessinstans(tidligstOpprettetProsessinstans)
        Assertions.assertThat(tidligstOpprettetProsessinstans.status).isEqualTo(ProsessStatus.KLAR)
    }

    private fun prosessinstans(registrertDato: LocalDateTime, referanse: String): Prosessinstans {
        val prosessinstans = Prosessinstans()
        prosessinstans.status = ProsessStatus.PÅ_VENT
        prosessinstans.låsReferanse = referanse
        prosessinstans.registrertDato = registrertDato
        return prosessinstans
    }
}
