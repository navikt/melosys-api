package no.nav.melosys.domain.dokument.felles;

public class Landkode {

    private String kode;

    // Brukes av JAXB
    public Landkode() {
    }

    public Landkode(String landkode) {
        this.kode = landkode;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }
    
}
