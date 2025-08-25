package no.nav.melosys.domain.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test

internal class KodeverkUtilsTest {

    @Test
    fun dekod() {
        val behandlingstype = KodeverkUtils.dekod(Behandlingstyper::class.java, Behandlingstyper.FØRSTEGANG.kode)


        behandlingstype shouldBe Behandlingstyper.FØRSTEGANG
    }

    @Test
    fun dekod_ikkeFunnet() {
        val exception = shouldThrow<IkkeFunnetException> {
            KodeverkUtils.dekod(Behandlingstyper::class.java, "ZØKNAD")
        }
        exception.message shouldContain "finnes ikke"
    }
}
