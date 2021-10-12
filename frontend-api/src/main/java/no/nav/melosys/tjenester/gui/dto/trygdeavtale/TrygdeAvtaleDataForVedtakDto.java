package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import java.time.LocalDate;
import java.util.List;

public record TrygdeAvtaleDataForVedtakDto(
    LocalDate fom,
    LocalDate tom,
    List<String> land,
    List<String> virksomheter,
    String vedtak,
    String innvilgelse,
    String bestemmelse,
    List<MedfolgendeFamilieDto> barn,
    MedfolgendeFamilieDto ektefelle
) {
}
