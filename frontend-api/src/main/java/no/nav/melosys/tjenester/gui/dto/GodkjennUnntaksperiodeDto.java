package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public record GodkjennUnntaksperiodeDto(boolean varsleUtland,
                                        String fritekst,
                                        PeriodeDto endretPeriode,
                                        String lovvalgsbestemmelse) { }
