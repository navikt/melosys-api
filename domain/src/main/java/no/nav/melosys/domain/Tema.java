package no.nav.melosys.domain;

public enum Tema implements Kodeverk {
    MED("MED"), // Medlemskap
    UFM("UFM"); // Unntak fra medlemskap

    private final String kode;

    Tema(String kode) {
        this.kode = kode;
    }

    @Override
    public String getKode() {
        return kode;
    }
}
