package no.nav.melosys.tjenester.gui.medlemskapsperiode.dto;

import no.nav.melosys.domain.kodeverk.Vilkaar;

import java.util.Collection;

public class VilkårOgBegrunnelserDto {
    private final Vilkaar vilkår;
    private final Collection<String> muligeBegrunnelser;

    public VilkårOgBegrunnelserDto(Vilkaar vilkår, Collection<String> muligeBegrunnelser) {
        this.vilkår = vilkår;
        this.muligeBegrunnelser = muligeBegrunnelser;
    }

    public Vilkaar getVilkår() {
        return vilkår;
    }

    public Collection<String> getMuligeBegrunnelser() {
        return muligeBegrunnelser;
    }
}
