package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "RETTIGHET_TYPE")
public class RettighetsType extends Kodeverk {

    private static final RettighetsType LOVVALGSLAND = new RettighetsType("LOVVALGSLAND");
    private static final RettighetsType FRIVILIG_MEDLEMSKAP = new RettighetsType("FRIVILIG_MEDL");
    private static final RettighetsType UNNTAK_MEDLEMSKAP = new RettighetsType("UNNTAK_MEDL");

    RettighetsType() {
    }

    private RettighetsType(String kode) {
        super(kode);
    }
}
