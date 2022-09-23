package no.nav.melosys.service.sak

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Sakstemaer.TRYGDEAVGIFT
import no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EndreSakServiceTest {
    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    lateinit var endreSakService: EndreSakService

    @BeforeEach
    fun setUp() {
        endreSakService = EndreSakService(fagsakService)
    }

    @Test
    fun endre() {
        val saksnummer = "MEL-123"

        endreSakService.endre(saksnummer, EU_EOS, TRYGDEAVGIFT)

        verify {fagsakService.oppdaterSakstype(saksnummer, EU_EOS)}
        verify {fagsakService.oppdaterSakstema(saksnummer, TRYGDEAVGIFT)}
    }
}
