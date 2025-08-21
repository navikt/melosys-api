package no.nav.melosys.domain.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import org.junit.jupiter.api.Test

internal class LovvalgBestemmelseUtilsTest {

    @Test
    fun dbDataTilLovvalgBestemmelseIkkeFunnet() {
        val exception = shouldThrow<IllegalArgumentException> {
            LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse("test")
        }
        exception.message shouldBe "Lovvalgbestemmelse kode:test ikke funnet"
    }

    @Test
    fun dbDataTilLovvalgBestemmelse() {
        val lovvalgBestemmelse = LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse("UK_ART7_3")


        lovvalgBestemmelse.run {
            kode shouldBe "UK_ART7_3"
            shouldBeInstanceOf<Lovvalgsbestemmelser_trygdeavtale_gb>()
        }
    }
}
