package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "BEHANDLING_TYPE")
public class BehandlingType extends Kodeverk {

    //TODO Francois: koder
    public static final BehandlingType FØRSTEGANGSSØKNAD = new BehandlingType("1");
    public static final BehandlingType ENDRINGSSØKNAD = new BehandlingType("2");
    public static final BehandlingType KLAGE = new BehandlingType("3");

    BehandlingType() {
    }

    private BehandlingType(String kode) {
        super(kode);
    }

}
