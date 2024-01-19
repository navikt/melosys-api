package no.nav.melosys.tjenester.gui.medlemskapsperiode.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record MedlemskapsperiodeOppdateringDto(LocalDate fomDato,
                                               LocalDate tomDato,
                                               Trygdedekninger trygdedekning,
                                               InnvilgelsesResultat innvilgelsesResultat,
                                               Folketrygdloven_kap2_bestemmelser bestemmelse) {
}
