package no.nav.melosys.domain;

public enum Territorier implements Omraade {

    FO("FO", "Færøyene"),
    GL("GL", "Grønland");

    private String kode;
    private String beskrivelse;

    Territorier(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
