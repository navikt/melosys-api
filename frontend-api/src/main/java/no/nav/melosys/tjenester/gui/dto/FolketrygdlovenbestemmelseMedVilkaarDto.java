package no.nav.melosys.tjenester.gui.dto;

import java.util.Collection;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vilkaar;

public class FolketrygdlovenbestemmelseMedVilkaarDto {
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final Collection<VilkårOgBegrunnelse> vilkårOgBegrunnelser;

    public FolketrygdlovenbestemmelseMedVilkaarDto(Folketrygdloven_kap2_bestemmelser bestemmelse, Collection<VilkårOgBegrunnelse> vilkårOgBegrunnelser) {
        this.bestemmelse = bestemmelse;
        this.vilkårOgBegrunnelser = vilkårOgBegrunnelser;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public Collection<VilkårOgBegrunnelse> getVilkårOgBegrunnelser() {
        return vilkårOgBegrunnelser;
    }

    public static class VilkårOgBegrunnelse {
        private final Vilkaar vilkaar;
        private final Collection<String> muligeBegrunnelser;

        public VilkårOgBegrunnelse(Vilkaar vilkaar, Collection<String> muligeBegrunnelser) {
            this.vilkaar = vilkaar;
            this.muligeBegrunnelser = muligeBegrunnelser;
        }

        public Vilkaar getVilkaar() {
            return vilkaar;
        }

        public Collection<String> getMuligeBegrunnelser() {
            return muligeBegrunnelser;
        }
    }
}