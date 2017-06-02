package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "BEHANDLING_MAATE")
public class BehandlingsMaate extends Kodeverk {

    public static final BehandlingsMaate AUTOMATISERT = new BehandlingsMaate("AUTO");
    public static final BehandlingsMaate DELVIS_AUTO = new BehandlingsMaate("DELVIS_AUTO");
    public static final BehandlingsMaate MANUELT = new BehandlingsMaate("MANUELT");

    BehandlingsMaate() {
    }

    private BehandlingsMaate (String kode) {
        super(kode);
    }

}
