package no.nav.melosys.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "BRUKER_KJOENN")
public class Kjoenn extends Kodeverk {

    public static final Kjoenn KVINNE = new Kjoenn("K");
    public static final Kjoenn MANN = new Kjoenn("M");

    Kjoenn() {
    }

    public Kjoenn(String kode) {
        super(kode);
    }

    public static final Kjoenn getFraKode(String kode) {

        if (KVINNE.getKode().equalsIgnoreCase(kode)) {
            return KVINNE;
        }

        if (MANN.getKode().equalsIgnoreCase(kode)) {
            return MANN;
        }

        throw new IllegalArgumentException("Kjønn :" + kode + " finnes ikke.");
    }

}
