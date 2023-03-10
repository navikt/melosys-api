package no.nav.melosys.tjenester.gui.medlemskapsperiode.dto;

import java.util.Collection;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;

public class BestemmelseMedVilkårOgBegrunnelserDto {
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final Collection<VilkårOgBegrunnelserDto> vilkårOgBegrunnelser;

    public BestemmelseMedVilkårOgBegrunnelserDto(Folketrygdloven_kap2_bestemmelser bestemmelse, Collection<VilkårOgBegrunnelserDto> vilkårOgBegrunnelser) {
        this.bestemmelse = bestemmelse;
        this.vilkårOgBegrunnelser = vilkårOgBegrunnelser;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public Collection<VilkårOgBegrunnelserDto> getVilkårOgBegrunnelser() {
        return vilkårOgBegrunnelser;
    }
}
