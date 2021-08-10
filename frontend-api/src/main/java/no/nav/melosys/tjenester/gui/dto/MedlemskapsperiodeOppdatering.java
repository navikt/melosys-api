package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record MedlemskapsperiodeOppdatering(LocalDate fomDato,
                                            LocalDate tomDato,
                                            Trygdedekninger trygdedekning,
                                            InnvilgelsesResultat innvilgelsesResultat) { }
