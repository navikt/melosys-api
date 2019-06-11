package no.nav.melosys.domain.eessi;

public class Institusjon {

    private final String id;
    private final String navn;
    private final String landkode;

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
}
