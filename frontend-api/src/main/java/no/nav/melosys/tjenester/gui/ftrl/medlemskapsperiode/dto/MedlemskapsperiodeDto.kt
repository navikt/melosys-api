package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Bestemmelse;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record MedlemskapsperiodeDto(long id,
                                    LocalDate fomDato,
                                    LocalDate tomDato,
                                    Bestemmelse bestemmelse,
                                    InnvilgelsesResultat innvilgelsesResultat,
                                    Trygdedekninger trygdedekning,
                                    Medlemskapstyper medlemskapstype) {

    public static MedlemskapsperiodeDto av(Medlemskapsperiode medlemskapsperiode) {
        return new MedlemskapsperiodeDto(
            medlemskapsperiode.getId(),
            medlemskapsperiode.getFom(),
            medlemskapsperiode.getTom(),
            medlemskapsperiode.getBestemmelse(),
            medlemskapsperiode.getInnvilgelsesresultat(),
            medlemskapsperiode.getTrygdedekning(),
            medlemskapsperiode.getMedlemskapstype()
        );
    }
}
