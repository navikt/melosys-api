package no.nav.melosys.service.kontroll.regler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class PeriodeReglerTest {

    @Test
    fun `feilIPeriode skal returnere false for gyldig periode`() {
        PeriodeRegler.feilIPeriode(DATO.plusMonths(6), DATO.plusYears(2)) shouldBe false
    }

    @Test
    fun `feilIPeriode skal returnere true når tom er før fom`() {
        PeriodeRegler.feilIPeriode(DATO.plusYears(1), DATO) shouldBe true
    }

    @Test
    fun `periodeErÅpen skal returnere true når ingen tildato`() {
        PeriodeRegler.periodeErÅpen(DATO.plusMonths(6), null) shouldBe true
    }

    @Test
    fun `periodeErÅpen skal returnere false når periode ikke er åpen`() {
        PeriodeRegler.periodeErÅpen(DATO.plusMonths(6), DATO.plusYears(1)) shouldBe false
    }

    @Test
    fun `periodeOver24Måneder skal returnere true for periode over 24 måneder`() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusYears(3)) shouldBe true
    }

    @Test
    fun `periodeOver24Måneder skal returnere true for periode på 24 måneder`() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusMonths(24)) shouldBe true
    }

    @Test
    fun `periodeOver24Måneder skal returnere false for periode på 23 måneder`() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusMonths(23)) shouldBe false
    }

    @Test
    fun `periodeOver24Måneder skal returnere false for periode på 14 måneder`() {
        PeriodeRegler.periodeOver24Måneder(DATO, DATO.plusMonths(14)) shouldBe false
    }

    @Test
    fun `periodeOver3År skal returnere false for periode på 2 år`() {
        PeriodeRegler.periodeOver3År(DATO, DATO.plusYears(2).plusMonths(11)) shouldBe false
    }

    @Test
    fun `periodeOver3År skal returnere true for periode på 3 år`() {
        PeriodeRegler.periodeOver3År(DATO, DATO.plusYears(3)) shouldBe true
    }

    @Test
    fun `periodeOver3År skal returnere true for periode på 4 år`() {
        PeriodeRegler.periodeOver3År(DATO, DATO.plusYears(4)) shouldBe true
    }

    @Test
    fun `periodeOver5År skal returnere true for periode på 5 år`() {
        PeriodeRegler.periodeOver5År(DATO, DATO.plusYears(5)) shouldBe true
    }

    @Test
    fun `periodeOver5År skal returnere false for periode på 4 år`() {
        PeriodeRegler.periodeOver5År(DATO, DATO.plusYears(4).plusMonths(11)) shouldBe false
    }

    @Test
    fun `datoEldreEnn3År skal returnere true for dato 6 år siden`() {
        PeriodeRegler.datoEldreEnn3År(LocalDate.now().minusYears(6)) shouldBe true
    }

    @Test
    fun `datoEldreEnn3År skal returnere false for dato fra nå`() {
        PeriodeRegler.datoEldreEnn3År(LocalDate.now()) shouldBe false
    }

    @Test
    fun `datoEldreEnn2Mnd skal returnere false for dato fra nå`() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe false
    }

    @Test
    fun `datoEldreEnn2Mnd skal returnere false for dato to måneder siden`() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe false
    }

    @Test
    fun `datoEldreEnn2Mnd skal returnere true for dato to måneder siden og en dag`() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(2).minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe true
    }

    @Test
    fun `datoEldreEnn2Mnd skal returnere true for dato tre måneder siden`() {
        PeriodeRegler.datoEldreEnn2Mnd(LocalDate.now().minusMonths(3).atStartOfDay(ZoneId.systemDefault()).toInstant()) shouldBe true
    }

    @Test
    fun `datoOver1ÅrFremITid skal returnere true for periode om 2 år`() {
        PeriodeRegler.datoOver1ÅrFremITid(LocalDate.now().plusYears(2)) shouldBe true
    }

    @Test
    fun `datoOver1ÅrFremITid skal returnere false for periode nå`() {
        PeriodeRegler.datoOver1ÅrFremITid(DATO) shouldBe false
    }

    @Test
    fun `periodeErLik skal returnere true for like perioder`() {
        PeriodeRegler.periodeErLik(DATO, DATO, DATO, DATO) shouldBe true
    }

    @Test
    fun `periodeErLik skal returnere false for ulike perioder`() {
        PeriodeRegler.periodeErLik(DATO, DATO.plusYears(1), DATO, DATO) shouldBe false
    }

    @Test
    fun `periodeErLik skal returnere true når tom er null for begge perioder`() {
        PeriodeRegler.periodeErLik(DATO, null, DATO, null) shouldBe true
    }

    @Test
    fun `periodeErLik skal returnere false når tom2 er null og tom1 ikke er det`() {
        PeriodeRegler.periodeErLik(DATO, DATO, DATO, null) shouldBe false
    }

    @Test
    fun `datoErFørFørsteJuni2012 skal returnere false for dato i 2020`() {
        PeriodeRegler.datoErFørFørsteJuni2012(LocalDate.of(2020, 1, 1)) shouldBe false
    }

    @Test
    fun `datoErFørFørsteJuni2012 skal returnere true for 1 januar 2012`() {
        PeriodeRegler.datoErFørFørsteJuni2012(LocalDate.of(2012, 1, 1)) shouldBe true
    }

    @Test
    fun `periodeOver12Måneder skal returnere true for periode på 12 måneder`() {
        PeriodeRegler.periodeOver12Måneder(DATO, DATO.plusMonths(12)) shouldBe true
    }

    companion object {
        private val DATO = LocalDate.parse("2024-01-01")
    }
} 