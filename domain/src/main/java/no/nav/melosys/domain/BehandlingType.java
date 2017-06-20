package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "BEHANDLING_TYPE")
public class BehandlingType extends Kodeverk {

    public static final BehandlingType FØRSTEGANGSSØKNAD = new BehandlingType("NY");
    public static final BehandlingType ENDRINGSSØKNAD = new BehandlingType("ENDRING");
    public static final BehandlingType KLAGE = new BehandlingType("KLAGE");

    BehandlingType() {
    }

    private BehandlingType(String kode) {
        super(kode);
    }

}
