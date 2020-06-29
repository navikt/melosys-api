package no.nav.melosys.domain.inngangsvilkar;

public class Feilmelding {

    private Kategori kategori;
    private String melding;

    public Kategori getKategori() {
        return kategori;
    }

    public void setKategori(Kategori kategori) {
        this.kategori = kategori;
    }

    public String getMelding() {
        return melding;
    }

    public void setMelding(String melding) {
        this.melding = melding;
    }
}
