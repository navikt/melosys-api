package no.nav.melosys.tjenester.gui.dto.periode;

import java.time.LocalDate;
import java.util.Map;

public class AnmodningsperiodeSvarDto {

    private final String anmodningsperiodeSvarType;
    private final LocalDate fom;
    private final LocalDate tom;
    private final String begrunnelseFritekst;

    public AnmodningsperiodeSvarDto(Map<String, String> json,/*json?*/ String anmodningsperiodeSvarType, LocalDate fom, LocalDate tom, String begrunnelseFritekst) {

        this.anmodningsperiodeSvarType = anmodningsperiodeSvarType;
        this.fom = fom;
        this.tom = tom;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }
}
