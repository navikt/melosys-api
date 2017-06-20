package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "VILKAAR_RESULTAT_UTFALL_TYPE")
public class VilkaarsResultatUtfallType extends Kodeverk {

    public static final VilkaarsResultatUtfallType OPPFYLT = new VilkaarsResultatUtfallType("OPPFYLT");
    public static final VilkaarsResultatUtfallType IKKE_OPPFYLT = new VilkaarsResultatUtfallType("IKKE_OPPFYLT");

    VilkaarsResultatUtfallType() {
    }

    private VilkaarsResultatUtfallType(String kode) {
        super(kode);
    }

}
