package no.nav.melosys.itest

import mu.KotlinLogging
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import no.nav.melosys.service.kodeverk.KodeverkService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@TestConfiguration
class KodeverkStub {
    @Bean
    @Primary
    fun kodeverkRegisterStub(): KodeverkRegister? = KodeverkRegister {
        Kodeverk(
            "DUMMY", mapOf(
                Pair(
                    "DUMMY",
                    listOf(Kode("DUMMY", "DUMMY", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)))
                )
            )
        )
    }

    @Bean
    @Primary
    fun kodeOppslagStub(): KodeOppslag? {
        open class KodeOppslagImpl : KodeOppslag {
            override fun getTermFraKodeverk(kodeverk: FellesKodeverk, kode: String): String = "DUMMY"

            override fun getTermFraKodeverk(kodeverk: FellesKodeverk, kode: String, dato: LocalDate): String =
                "DUMMY"

            override fun getTermFraKodeverk(
                kodeverk: FellesKodeverk,
                kode: String,
                dato: LocalDate,
                kodeperioder: List<Kode>?
            ): String = "DUMMY"
        }

        return KodeOppslagImpl()
    }

    @Bean
    @Primary
    fun kodeverkServiceStub(kodeverkRegister: KodeverkRegister?, kodeOppslag: KodeOppslag?): KodeverkService? {
        log.info("kodeverkServiceStub lastet")
        return KodeverkService(kodeverkRegister, kodeOppslag)
    }
}
