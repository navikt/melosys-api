package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "kontaktopplysning")
public class Kontaktopplysning {
    @EmbeddedId
    private KontaktopplysningID kontaktopplysningID;

    @Column(name = "kontakt_navn")
    private String kontaktNavn;

    @Column(name = "kontakt_orgnr")
    private String kontaktOrgnr;

    public KontaktopplysningID getKontaktopplysningID() {
        return kontaktopplysningID;
    }

    public void setKontaktopplysningID(KontaktopplysningID kontaktopplysningID) {
        this.kontaktopplysningID = kontaktopplysningID;
    }

    public String getKontaktNavn() {
        return kontaktNavn;
    }

    public void setKontaktNavn(String kontaktNavn) {
        this.kontaktNavn = kontaktNavn;
    }

    public String getKontaktOrgnr() {
        return kontaktOrgnr;
    }

    public void setKontaktOrgnr(String kontaktOrgnr) {
        this.kontaktOrgnr = kontaktOrgnr;
    }

    public static Kontaktopplysning av(String orgnr, String kontaktNavn) {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        KontaktopplysningID kontaktopplysningID = new KontaktopplysningID();
        kontaktopplysningID.setOrgnr(orgnr);
        kontaktopplysning.setKontaktopplysningID(kontaktopplysningID);
        kontaktopplysning.setKontaktNavn(kontaktNavn);
        return kontaktopplysning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Kontaktopplysning)) return false;
        Kontaktopplysning that = (Kontaktopplysning) o;
        return Objects.equals(getKontaktopplysningID(), that.getKontaktopplysningID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKontaktopplysningID());
    }
}
