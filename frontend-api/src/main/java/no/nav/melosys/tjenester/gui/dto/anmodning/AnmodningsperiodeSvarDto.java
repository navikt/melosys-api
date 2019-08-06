package no.nav.melosys.tjenester.gui.dto.anmodning;

import java.time.LocalDate;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public class AnmodningsperiodeSvarDto {

    public final String anmodningsperiodeSvarType;
    public final PeriodeDto endretPeriode;
    public final String begrunnelseFritekst;

    public AnmodningsperiodeSvarDto() {
        this(null, new PeriodeDto(), null);
    }

    public AnmodningsperiodeSvarDto(String anmodningsperiodeSvarType, PeriodeDto endretPeriode, String begrunnelseFritekst) {
        this.anmodningsperiodeSvarType = anmodningsperiodeSvarType;
        this.endretPeriode = endretPeriode;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public final AnmodningsperiodeSvar til() {
        return new AnmodningsperiodeSvar(
            null,
            enumVerdiEllerNull(AnmodningsperiodeSvarType.class, anmodningsperiodeSvarType),
            LocalDate.now(),
            begrunnelseFritekst,
            endretPeriode != null ? endretPeriode.getFom() : null,
            endretPeriode != null ? endretPeriode.getTom() : null
        );
    }

    public static AnmodningsperiodeSvarDto fra(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        return new AnmodningsperiodeSvarDto(
            anmodningsperiodeSvar.getAnmodningsperiodeSvarType().getKode(),
            new PeriodeDto(anmodningsperiodeSvar.getInnvilgetFom(), anmodningsperiodeSvar.getInnvilgetTom()),
            anmodningsperiodeSvar.getBegrunnelseFritekst()
        );
    }

    static <E extends Enum<E>> E enumVerdiEllerNull(Class<E> enumKlasse, String nøkkel) {
        return nøkkel == null ? null : Enum.valueOf(enumKlasse, nøkkel);
    }
}
