package no.nav.melosys.tjenester.gui

import java.time.LocalDate

import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import no.nav.melosys.service.kodeverk.KodeverkService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TestApplication {

    @Bean
    fun kodeverkRegisterStub(): KodeverkRegister = object : KodeverkRegister {
        override fun hentKodeverk(kodeverkNavn: String): Kodeverk {
            val kode = Kode("DUMMY", "DUMMY", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            val kodeMap = mapOf("DUMMY" to listOf(kode))
            return Kodeverk("DUMMY", kodeMap)
        }
    }

    @Bean
    fun kodeverkService(kodeverkRegister: KodeverkRegister): KodeverkService = 
        KodeverkService(kodeverkRegister)
}