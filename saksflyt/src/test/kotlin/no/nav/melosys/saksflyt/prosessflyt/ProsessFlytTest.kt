package no.nav.melosys.saksflyt.prosessflyt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.Test


internal class ProsessFlytTest {
    private val førsteSteg = ProsessSteg.LAGRE_ANMODNINGSPERIODE_MEDL
    private val andreSteg = ProsessSteg.OPPRETT_ARKIVSAK
    private val sisteSteg = ProsessSteg.VIDERESEND_SØKNAD

    @Test
    fun nesteSteg_forrigeStegErNull_forventFørsteElement() {
        val prosessFlyt = lagProsessFlyt()
        prosessFlyt.nesteSteg(null) shouldBe førsteSteg
    }

    @Test
    fun nesteSteg_forrigeStegErFørsteElement_forventAndreElement() {
        val prosessFlyt = lagProsessFlyt()
        prosessFlyt.nesteSteg(førsteSteg) shouldBe andreSteg
    }

    @Test
    fun nesteSteg_forrigeStegErSisteElement_forventNull() {
        val prosessFlyt = lagProsessFlyt()
        prosessFlyt.nesteSteg(sisteSteg) shouldBe null
    }

    @Test
    fun nesteSteg_forrigeStegIkkeEnDelAvFlyt_kasterException() {
        shouldThrow<IllegalArgumentException> {
            val prosessFlyt = lagProsessFlyt()
            prosessFlyt.nesteSteg(ProsessSteg.AVSLUTT_TIDLIGERE_MEDL_PERIODE)
        }.message.shouldContain("ikke gyldig for prosesstype")
    }

    @Test
    fun opprettProsessflyt_medDuplikatSteg_kasterException() {
        shouldThrow<IllegalArgumentException> {
            ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK, førsteSteg, andreSteg, sisteSteg, førsteSteg)
        }.message.shouldContain("er definert to eller flere ganger")
    }

    private fun lagProsessFlyt(): ProsessFlyt {
        return ProsessFlyt(ProsessType.ANMODNING_OM_UNNTAK, førsteSteg, andreSteg, sisteSteg)
    }
}
