package no.nav.melosys.domain.behandlingsgrunnlag.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import no.nav.melosys.exception.TekniskException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class MedfolgendeFamilieTest {

    private static final String NAVN = "Doffen Duck";

    @Test
    void datoFraFnr_gyldigFnr_kastException() {
        MedfolgendeFamilie medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn("01108049800", NAVN);
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(medfolgendeFamilie::datoFraFnr)
            .withMessageContaining("Kan bare parse dato når IdentType er DATO");
    }

    @Test
    void datoFraFnr_gyldigFnrDato_kastException() {
        MedfolgendeFamilie medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn("20.13.21", NAVN);
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(medfolgendeFamilie::datoFraFnr)
            .withMessageContaining("fnr: 20.13.21 kan ikke parsers til fødselsdato");
    }

    @ParameterizedTest()
    @MethodSource("gyldigeDatoer")
    void datoFraFnr_parseDatoMedGyldigFormat_returnerDato(String dato, LocalDate expected) {
        MedfolgendeFamilie medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn(dato, NAVN);
        assertThat(medfolgendeFamilie.datoFraFnr()).isEqualTo(expected);
    }

    @ParameterizedTest()
    @MethodSource("århundreOvergangDatoer")
    void datoFraFnr_ÅrhundreOvergangDatoer_returnerDato(String dato, LocalDate expected) {
        MedfolgendeFamilie medfolgendeFamilie = MedfolgendeFamilie.tilBarnFraFnrOgNavn(dato, NAVN);
        assertThat(medfolgendeFamilie.datoFraFnr()).isEqualTo(expected);
        System.out.println(medfolgendeFamilie.datoFraFnr());
    }

    private static List<Arguments> gyldigeDatoer() {
        return Stream.of(
            "01021980",
            "010280",
            "01.02.80",
            "01.02.1980",
            "01/02/80",
            "01/02/1980",
            "01-02-80",
            "01-02-1980"
        ).map(s -> Arguments.of(s, LocalDate.of(1980, 2, 1))).toList();
    }

    private static List<Arguments> århundreOvergangDatoer() {
        LocalDate now = LocalDate.now();
        LocalDate bruk2000 = now;
        LocalDate bruk1900 = now.plusDays(1);
        return List.of(
            Arguments.of((bruk2000.format(DateTimeFormatter.ofPattern("dd.MM.YY"))), bruk2000),
            Arguments.of((bruk1900.format(DateTimeFormatter.ofPattern("dd.MM.YY"))), bruk1900.minusYears(100)),
            Arguments.of(("01.02.21"), LocalDate.of(2021, 2, 1))
        );
    }
}
