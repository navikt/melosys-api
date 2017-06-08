package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "REGEL_TYPE")
public class RegelType extends Kodeverk {

    public static final RegelType AVKLARE_FAKTA = new RegelType("AVKLARE_FAKTA");
    public static final RegelType BEREGNING = new RegelType("BEREGNING");
    public static final RegelType FORRETNING = new RegelType("FORRETNING");
    public static final RegelType VILKAAR = new RegelType("VILKAAR");

    RegelType() {
    }

    private RegelType(String kode) {
        super(kode);
    }

}
