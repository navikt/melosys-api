package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.service.unntaksperiode.Unntaksperiode;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;

public record GodkjennUnntaksperiodeDto(boolean varsleUtland,
                                        String fritekst,
                                        PeriodeDto endretPeriode,
                                        String lovvalgsbestemmelse) {

    public UnntaksperiodeGodkjenning til() {
        final LovvalgBestemmelsekonverterer lovvalgBestemmelsekonverterer = new LovvalgBestemmelsekonverterer();

        return UnntaksperiodeGodkjenning.builder()
            .varsleUtland(this.varsleUtland)
            .fritekst(this.fritekst)
            .unnntaksperiode(new Unntaksperiode(this.endretPeriode.getFom(), this.endretPeriode.getTom()))
            .lovvalgsbestemmelse(lovvalgBestemmelsekonverterer.convertToEntityAttribute(this.lovvalgsbestemmelse))
            .build();
    }
}
