package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity(name = "SaksopplysningKilde")
@Table(name = "SAKSOPPLYSNING_KILDE")
public class SaksopplysningKilde extends Kodeverk {

    public static final SaksopplysningKilde TPS = new SaksopplysningKilde("TPS");
    public static final SaksopplysningKilde JOARK = new SaksopplysningKilde("JOARK");

    SaksopplysningKilde() {
    }

    private SaksopplysningKilde(String kode) {
        super(kode);
    }
}
