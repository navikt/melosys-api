package no.nav.melosys.saksflyt.metrikker

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.doubles.shouldBePositive
import io.kotest.matchers.doubles.shouldBeZero
import io.mockk.every
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus.FEILET
import no.nav.melosys.saksflytapi.domain.ProsessStatus.FERDIG
import no.nav.melosys.saksflytapi.domain.ProsessType.ANMODNING_OM_UNNTAK
import no.nav.melosys.saksflytapi.domain.ProsessType.IVERKSETT_VEDTAK_EOS
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration

@SpringBootTest(classes = [ProsessinstansStatusCache::class])
@EnableScheduling
@TestPropertySource(properties = ["melosys.prosesser.status.oppfriskning.frekvens=100"])
@ExtendWith(SpringExtension::class)
class ProsessinstansStatusCacheTest {

    @MockkBean
    private lateinit var prosessinstansRepository: ProsessinstansRepository

    @Autowired
    private lateinit var cache: ProsessinstansStatusCache

    @Test
    @Disabled("Er ustabil så feiler litt random")
    fun `antallProsessinstanserFeiletPåType skal oppdateres korrekt`() {
        every { prosessinstansRepository.antallAktiveOgFeiletPerTypeOgStatus(any()) } returns getProsessinstansAntallFeilet()
        every { prosessinstansRepository.antallAktiveOgFeiletPerStegOgStatus(any(), true) } returns emptyList()

        cache.antallProsessinstanserFeiletPåType(ANMODNING_OM_UNNTAK).shouldBeZero()

        await().atMost(Duration.ofSeconds(1))
            .pollDelay(Duration.ofMillis(50))
            .untilAsserted {
                cache.antallProsessinstanserFeiletPåType(ANMODNING_OM_UNNTAK).shouldBePositive()
            }
    }

    private fun getProsessinstansAntallFeilet(): Collection<ProsessinstansAntall> = mutableListOf(
        ProsessinstansAntall(ANMODNING_OM_UNNTAK, FERDIG, 0),
        ProsessinstansAntall(ANMODNING_OM_UNNTAK, FEILET, 2),
        ProsessinstansAntall(IVERKSETT_VEDTAK_EOS, FERDIG, 2)
    )
}
