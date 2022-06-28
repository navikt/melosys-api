package no.nav.melosys.integrasjon.doksys.dokumentproduksjon

import no.nav.melosys.sikkerhet.sts.StsLoginConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest(
    value = [
        StsLoginConfig::class
    ],
    properties = ["spring.profiles.active:token-test"]
)
class DokumentproduksjonConsumerSimpleTest(
    @Autowired stsLoginConfig: StsLoginConfig
) {
    @Test
    fun test() {
    }
}
