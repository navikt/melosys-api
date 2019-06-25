package no.nav.melosys.tjenester.gui.dto.periode;

import java.time.LocalDate;

import no.nav.melosys.domain.anmodningsperiode.AnmodningsperiodeSvar;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;

public class AnmodningsperiodeSvarDto {

    private final String anmodningsperiodeSvarType;
    private final LocalDate fom;
    private final LocalDate tom;
    private final String begrunnelseFritekst;

    public AnmodningsperiodeSvarDto(String anmodningsperiodeSvarType, LocalDate fom, LocalDate tom, String begrunnelseFritekst) {
        this.anmodningsperiodeSvarType = anmodningsperiodeSvarType;
        this.fom = fom;
        this.tom = tom;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public final AnmodningsperiodeSvar til() {
        return new AnmodningsperiodeSvar(
            null,
            enumVerdiEllerNull(AnmodningsperiodeSvarType.class, anmodningsperiodeSvarType),
            null,
            begrunnelseFritekst,
            fom,
            tom
        );
    }

    public static AnmodningsperiodeSvarDto fra(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        return new AnmodningsperiodeSvarDto(
            anmodningsperiodeSvar.getAnmodningsperiodeSvarType().getKode(),
            anmodningsperiodeSvar.getFom(),
            anmodningsperiodeSvar.getTom(),
            anmodningsperiodeSvar.getBegrunnelseFritekst()
        );
    }

    static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}
