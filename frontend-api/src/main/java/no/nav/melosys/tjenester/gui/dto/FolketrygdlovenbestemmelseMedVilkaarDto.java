package no.nav.melosys.tjenester.gui.dto;

import java.util.Collection;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vilkaar;

public class FolketrygdlovenbestemmelseMedVilkaarDto {
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final Collection<Vilkaar> vilkår;

    public FolketrygdlovenbestemmelseMedVilkaarDto(Folketrygdloven_kap2_bestemmelser bestemmelse, Collection<Vilkaar> vilkår) {
        this.bestemmelse = bestemmelse;
        this.vilkår = vilkår;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public Collection<Vilkaar> getVilkår() {
        return vilkår;
    }
}