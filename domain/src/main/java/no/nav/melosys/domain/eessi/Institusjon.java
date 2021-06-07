package no.nav.melosys.domain.eessi;

import java.util.Objects;

public class Institusjon {

    private String id;
    private String navn;
    private String landkode;

    public Institusjon(){
    }

    public Institusjon(String id, String navn, String landkode) {
        this.id = id;
        this.navn = navn;
        this.landkode = landkode;
    }

    public String getId() {
        return id;
    }

    public String getNavn() {
        return navn;
    }

    public String getLandkode() {
        return landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Institusjon)) return false;
        Institusjon institusjon = (Institusjon) o;
        return getId().equals(institusjon.getId()) &&
            Objects.equals(getNavn(), institusjon.getNavn()) &&
            Objects.equals(getLandkode(), institusjon.getLandkode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNavn(), getLandkode());
    }
}
