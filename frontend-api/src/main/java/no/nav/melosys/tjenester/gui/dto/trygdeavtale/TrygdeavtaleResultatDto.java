package no.nav.melosys.tjenester.gui.dto.trygdeavtale;

import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import java.util.List;

public record TrygdeavtaleResultatDto(
    List<String> virksomheter,
    String bestemmelse,
    List<MedfolgendeFamilieDto> barn,
    MedfolgendeFamilieDto ektefelle
) {
    public TrygdeavtaleService.TrygdeavtaleResultat til() {
        return new TrygdeavtaleService.TrygdeavtaleResultat.Builder()
            .barn(this.barn.stream()
                .map(b -> new TrygdeavtaleService.Familie(b.uuid(), b.omfattet(), b.begrunnelseKode(), b.begrunnelseFritekst()))
                .toList())
            .ektefelle(ektefelle.uuid(), ektefelle.omfattet(), ektefelle.begrunnelseKode(), ektefelle.begrunnelseFritekst())
            .bestemmelse(bestemmelse)
            .virksomheter(virksomheter)
            .build();
    }
}
