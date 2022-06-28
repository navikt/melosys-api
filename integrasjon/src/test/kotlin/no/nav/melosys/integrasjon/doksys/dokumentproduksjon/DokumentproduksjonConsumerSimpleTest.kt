package no.nav.melosys.integrasjon.doksys.dokumentproduksjon

import no.nav.melosys.sikkerhet.sts.StsLoginConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import

@WebMvcTest(
    value = [
        StsLoginConfig::class
    ],
    properties = ["spring.profiles.active:token-test"]
)
@Import(StsLoginConfig::class)
class DokumentproduksjonConsumerSimpleTest(
    @Autowired private val stsLoginConfig: StsLoginConfig
) {
    @Test
    fun test() {
        println(stsLoginConfig.stsPolicy)
    }
}
