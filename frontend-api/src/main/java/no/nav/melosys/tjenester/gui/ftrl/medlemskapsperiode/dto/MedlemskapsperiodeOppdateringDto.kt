package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record MedlemskapsperiodeOppdateringDto(LocalDate fomDato,
                                               LocalDate tomDato,
                                               String bestemmelse,
                                               Trygdedekninger trygdedekning,
                                               InnvilgelsesResultat innvilgelsesResultat) {
}
