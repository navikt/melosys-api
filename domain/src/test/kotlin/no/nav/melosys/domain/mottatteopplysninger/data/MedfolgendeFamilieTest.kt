package no.nav.melosys.domain.mottatteopplysninger.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

internal class MedfolgendeFamilieTest {

    @Test
    fun datoFraFnr_gyldigFnr_returnerFødselsdato() {
        val medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn("01108049800", NAVN)


        medfolgendeFamilie.datoFraFnr() shouldBe LocalDate.of(1980, 10, 1)
    }

    @Test
    fun datoFraFnr_feilDatoFormat_kastException() {
        val medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn("20.13.21", NAVN)


        val exception = shouldThrow<TekniskException> {
            medfolgendeFamilie.datoFraFnr()
        }
        exception.message shouldContain "fnr: 20.13.21 kan ikke parsers til fødselsdato"
    }

    @ParameterizedTest
    @MethodSource("gyldigeDatoer")
    fun datoFraFnr_parseDatoMedGyldigFormat_returnerDato(dato: String, expected: LocalDate) {
        val medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn(dato, NAVN)


        medfolgendeFamilie.datoFraFnr() shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("århundreOvergangDatoer")
    fun datoFraFnr_ÅrhundreOvergangDatoer_returnerDato(dato: String, expected: LocalDate) {
        val medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn(dato, NAVN)


        medfolgendeFamilie.datoFraFnr() shouldBe expected
    }

    companion object {
        private const val NAVN = "Doffen Duck"

        @JvmStatic
        fun gyldigeDatoer(): List<Arguments> =
            Stream.of(
                "01021980",
                "010280",
                "01.02.80",
                "01.02.1980",
                "01/02/80",
                "01/02/1980",
                "01-02-80",
                "01-02-1980"
            ).map { s -> Arguments.of(s, LocalDate.of(1980, 2, 1)) }.toList()

        @JvmStatic
        fun århundreOvergangDatoer(): List<Arguments> {
            val now = LocalDate.now()
            val bruk2000 = now
            val bruk1900 = now.plusDays(1)
            return listOf(
                Arguments.of(bruk2000.format(DateTimeFormatter.ofPattern("dd.MM.YY")), bruk2000),
                Arguments.of(bruk1900.format(DateTimeFormatter.ofPattern("dd.MM.YY")), bruk1900.minusYears(100)),
                Arguments.of("01.02.21", LocalDate.of(2021, 2, 1))
            )
        }
    }
}
