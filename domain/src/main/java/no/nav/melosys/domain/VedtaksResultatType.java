package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "VEDTAK_RESULTAT_TYPE")
public class VedtaksResultatType extends Kodeverk {

    public static final VedtaksResultatType INNVILGET = new VedtaksResultatType("INNVILGET");
    public static final VedtaksResultatType DELVIS_INNVILGET = new VedtaksResultatType("DELVIS_INNVILGET");
    public static final VedtaksResultatType AVSLAG = new VedtaksResultatType("AVSLAG");

    private VedtaksResultatType() {
    }

    public VedtaksResultatType(String kode) {
        super(kode);
    }
}
