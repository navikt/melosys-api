package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "FAGSAK_STATUS")
public class FagsakStatus extends Kodeverk {

    public static final FagsakStatus OPPRETTET = new FagsakStatus("OPPR");
    public static final FagsakStatus UBEH = new FagsakStatus("UBEH");
    public static final FagsakStatus LØPENDE = new FagsakStatus("LOP");
    public static final FagsakStatus AVSLUTTET = new FagsakStatus("AVSLU");

    FagsakStatus() {
    }

    private FagsakStatus(String kode) {
        super(kode);
    }

}
