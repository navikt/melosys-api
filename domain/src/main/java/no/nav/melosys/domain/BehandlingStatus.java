package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "BEHANDLING_STATUS")
public class BehandlingStatus extends Kodeverk {

    public static final BehandlingStatus OPPRETTET = new BehandlingStatus("OPPR");
    public static final BehandlingStatus UTREDES = new BehandlingStatus("UTRED");
    public static final BehandlingStatus FATTER_VEDTAK = new BehandlingStatus("F_VED");
    public static final BehandlingStatus IVERKSETTER_VEDTAK = new BehandlingStatus("I_VED");
    public static final BehandlingStatus AVSLUTTET = new BehandlingStatus("AVSLU");

    public BehandlingStatus() {
    }

    private BehandlingStatus(String kode) {
        super(kode);
    }

}
