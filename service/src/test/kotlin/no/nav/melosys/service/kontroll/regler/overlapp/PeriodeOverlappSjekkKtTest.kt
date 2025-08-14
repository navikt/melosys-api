package no.nav.melosys.service.kontroll.regler.overlapp

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.dokument.medlemskap.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PeriodeOverlappSjekkKtTest {

    @Test
    fun `har periode som overlapper mer enn 1 dag medlemsperiode overlapper kontrollperiode med 2 år og 1 dag true`() {
        /*
            Overlap:                       1d
            medlemsperiode:        |------|::|              2y
            kontrollperiode:              |::|------|       2y
        */
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1))
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(medlemsperiode, kontrollperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode overlapper medlemsperiode med 2 år og 1 dag true`() {
        /*
            Overlap:                        1d
            kontrollperiode:               |::|------|          2y
            medlemsperiode:         |------|::|                 2y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag medlemsperiode overlapper kontrollperiode har 2 dager over samme start og slutt dag på 2 år true`() {
        /*
            Overlap:                       1d+1d
            medlemsperiode:        |------|::|::|               2y
            kontrollperiode:              |::|::|------|        2y
        */
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(2))
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(medlemsperiode, kontrollperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode overlapper medlemsperiode har 2 dager over samme start og slutt dag på 2 år true`() {
        /*
            Overlap:                        1d+1d
            kontrollperiode:               |::|::|------|       2y
            medlemsperiode:         |------|::|::|              2y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag medlemsperiode overlapper med kontrollperiode har samme start og slutt dag på 2 år false`() {
        /*
            Overlap:                      1d
            kontrollperiode:        |------|            2y
            medlemsperiode:                |------|     2y
        */
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(medlemsperiode, kontrollperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode har samme start og slutt dag på 2 år false`() {
        /*
            Overlap:                       1d
            kontrollperiode:               |------|     2y
            medlemsperiode:         |------|            2y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag medlemsperiode overlapper med kontrollperiode har samme start og slutt dag på 1 år false`() {
        /*
            Overlap:                      1d
            kontrollperiode:        |------|            1y
            medlemsperiode:                |------|     1y
        */
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1))
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(medlemsperiode, kontrollperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode har samme start og slutt dag på 1 år false`() {
        /*
            Overlap:                      1d
            kontrollperiode:               |------|     1y
            medlemsperiode:         |------|            1y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag medlemsperiode overlapper ikke kontrollperiode har ingen overlapp false`() {
        /*
            Overlap:                         (1d)
            kontrollperiode:        |------|                1y
            medlemsperiode:                      |------|   1y
        */
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1))
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(1).plusDays(1), LocalDate.EPOCH.plusYears(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(medlemsperiode, kontrollperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper ikke medlemsperiode har ingen overlapp false`() {
        /*
            Overlap:                        (1d)
            kontrollperiode:                    |------|    1y
            medlemsperiode:         |------|                1y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(1).plusDays(1), LocalDate.EPOCH.plusYears(2))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med start og slutt overlapper hverandre true`() {
        /*
            Overlap:                1d+1d
            kontrollperiode:        |:::|       2d
            medlemsperiode:         |:::|       2d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode overlapper medlemsperiode med samme slutt og start dato overlapp false`() {
        /*
            Overlap:                   1d
            kontrollperiode:        |--|        2d
            medlemsperiode:            |--|     2d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1))
        val medlemsperiode = Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag medlemsperiode overlapper kontrollperiode med samme slutt og start dato overlapp false`() {
        /*
            Overlap:                  1d
            kontrollperiode:           |--|     2d
            medlemsperiode:         |--|        2d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(2))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med 1 dag overlapping starten av 10 dager true`() {
        /*
            Overlap:                1d+1d
            kontrollperiode:        |::|                                 2d
            medlemsperiode:         |::|--|--|--|--|--|--|--|--|         10d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med 1 dag overlapping slutten av 10 dager true`() {
        /*
            Overlap:                                        1d+1d
            kontrollperiode:                                |::|     2d
            medlemsperiode:         |--|--|--|--|--|--|--|--|::|     10d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusDays(9), LocalDate.EPOCH.plusDays(10))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode som overlapper med en dag mellom medlemsperioden false`() {
        /*
            Overlap:                                        1d
            kontrollperiodePåEnDag                          |        1d
            medlemsperiode:         |--|--|--|--|--|--|--|--|--|     10d
        */
        val kontrollperiodePåEnDag = Periode(LocalDate.EPOCH.plusDays(9), LocalDate.EPOCH.plusDays(9))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiodePåEnDag, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med 2 dag overlapping mellom 10 dager true`() {
        /*
            Overlap:                         1d+1d
            kontrollperiode:                 |::|                    2d
            medlemsperiode:         |--|--|--|::|--|--|--|--|--|     10d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusDays(3), LocalDate.EPOCH.plusDays(4))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med 3 dager overlapping mellom 10 dager true`() {
        /*
            Overlap:                         1d+1d+1d
            kontrollperiode:                 |::|::|                 3d
            medlemsperiode:         |--|--|--|::|::|--|--|--|--|     10d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusDays(3), LocalDate.EPOCH.plusDays(5))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med 2 dag overlapping over 3 dager true`() {
        /*
            Overlap:                   1d+1d
            kontrollperiode:        |--|::|         3d
            medlemsperiode:            |::|--|      3d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2))
        val medlemsperiode = Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(3))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontroll periode overlapper medlemsperiode med 3 dager overlapping over 3 dager true`() {
        /*
            Overlap:                1d+1d+1d
            kontrollperiode:        |::|::|     3d
            medlemsperiode:         |::|::|     3d
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2))
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode har åpen overlapp med medlemsperiode på 2 dager true`() {
        /*
            Overlap:                       1d+1d
            kontrollperiode:               |::|-->          åpen periode
            medlemsperiode:         |------|::|             2y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), null)
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode har åpen overlapp med medlemsperiode på 1 dag false`() {
        /*
            Overlap:                          1d
            kontrollperiode:                  |-->          åpen periode
            medlemsperiode:         |---------|             2y
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(2), null)
        val medlemsperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode har 2 dager overlapp med åpen medlemsperiode true`() {
        /*
            Overlap:                       1d+1d
            kontrollperiode:        |------|::|             2y
            medlemsperiode:                |::|-->          åpen periode
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1))
        val medlemsperiode = Periode(LocalDate.EPOCH.plusYears(2), null)
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag kontrollperiode har 1 dag overlapp med åpen medlemsperiode false`() {
        /*
            Overlap:                          1d
            kontrollperiode:        |---------|             2y
            medlemsperiode:                   |-->          åpen periode
        */
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        val medlemsperiode = Periode(LocalDate.EPOCH.plusYears(2), null)
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeFalse()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag åpen medlemsperiode overlapper hele kontrollperioden true`() {
        /*
            Overlap:            1d          2y
            kontrollperiode:           |:::::::::|             2y
            medlemsperiode:      |------:::::::::->            åpen periode
        */
        val kontrollperiode = Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(3))
        val medlemsperiode = Periode(LocalDate.EPOCH, null)
        val periodeOverlappSjekk = PeriodeOverlappSjekk(kontrollperiode, medlemsperiode)


        val periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag()


        periodeOverlapperMerEnn1Dag.shouldBeTrue()
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag mangler fom på medlemsperiode kaster IllegalArgumentException`() {
        val kontrollperiode = Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1))
        val medlemsperiodeUtenFom = Periode(null, LocalDate.EPOCH.plusYears(2))

        shouldThrow<IllegalArgumentException> {
            PeriodeOverlappSjekk(kontrollperiode, medlemsperiodeUtenFom)
        }
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag mangler fom på kontrollperiode kaster IllegalArgumentException`() {
        val kontrollperiodeUtenFom = Periode(null, LocalDate.EPOCH.plusYears(1))
        val medlemsperiode = Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2))

        shouldThrow<IllegalArgumentException> {
            PeriodeOverlappSjekk(kontrollperiodeUtenFom, medlemsperiode)
        }
    }

    @Test
    fun `har periode som overlapper mer enn 1 dag mangler tom på begge perioder kaster IllegalArgumentException`() {
        val kontrollperiodeUtenTom = Periode(LocalDate.EPOCH, null)
        val medlemsperiodeUtenTom = Periode(LocalDate.EPOCH.plusYears(1), null)

        shouldThrow<IllegalArgumentException> {
            PeriodeOverlappSjekk(kontrollperiodeUtenTom, medlemsperiodeUtenTom)
        }
    }
}