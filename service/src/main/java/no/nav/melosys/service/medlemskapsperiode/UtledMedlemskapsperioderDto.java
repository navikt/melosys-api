package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class UtledMedlemskapsperioderDto {

    private final ErPeriode søknadsperiode;
    private final Trygdedekninger trygdedekning;
    private final LocalDate mottaksdatoSøknad;
    private final String arbeidsland;

    public UtledMedlemskapsperioderDto(ErPeriode søknadsperiode,
                                       Trygdedekninger trygdedekning,
                                       LocalDate mottaksdatoSøknad,
                                       String arbeidsland) {
        this.søknadsperiode = søknadsperiode;
        this.trygdedekning = trygdedekning;
        this.mottaksdatoSøknad = mottaksdatoSøknad;
        this.arbeidsland = arbeidsland;
    }

    public ErPeriode getSøknadsperiode() {
        return søknadsperiode;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public LocalDate getMottaksdatoSøknad() {
        return mottaksdatoSøknad;
    }

    public String getArbeidsland() {
        return arbeidsland;
    }

    public static UtledMedlemskapsperioderDto av(UtledMedlemskapsperiodeNyVurderingDto dto) {
        return new UtledMedlemskapsperioderDto(dto.getSøknadsperiode(), dto.getTrygdedekning(), dto.getMottaksdatoSøknad(), dto.getArbeidsland());
    }

    public static UtledMedlemskapsperioderDto av(UtledMedlemskapsperiodeNyVurderingDto dto, ErPeriode søknadsperiode) {
        return new UtledMedlemskapsperioderDto(søknadsperiode, dto.getTrygdedekning(), dto.getMottaksdatoSøknad(), dto.getArbeidsland());
    }
}
