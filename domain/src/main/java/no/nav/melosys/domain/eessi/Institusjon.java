package no.nav.melosys.domain.eessi;

public class Institusjon {

    private String id;
    private String navn;
    private String landkode;

    public Institusjon(String id, String navn, String landkode) {
        this.id = id;
        this.navn = navn;
        this.landkode = landkode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String landkode) {
        this.landkode = landkode;
    }
}
