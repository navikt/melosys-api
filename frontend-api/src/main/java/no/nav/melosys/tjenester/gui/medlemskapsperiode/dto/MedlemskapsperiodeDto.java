package no.nav.melosys.tjenester.gui.medlemskapsperiode.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record MedlemskapsperiodeDto(long id,
                                    String arbeidsland,
                                    LocalDate fomDato,
                                    LocalDate tomDato,
                                    InnvilgelsesResultat innvilgelsesResultat,
                                    Trygdedekninger trygdedekning,
                                    Medlemskapstyper medlemskapstype) {

    public static MedlemskapsperiodeDto av(Medlemskapsperiode medlemskapsperiode) {
        return new MedlemskapsperiodeDto(
            medlemskapsperiode.getId(),
            medlemskapsperiode.getArbeidsland(),
            medlemskapsperiode.getFom(),
            medlemskapsperiode.getTom(),
            medlemskapsperiode.getInnvilgelsesresultat(),
            medlemskapsperiode.getTrygdedekning(),
            medlemskapsperiode.getMedlemskapstype()
        );
    }
}
