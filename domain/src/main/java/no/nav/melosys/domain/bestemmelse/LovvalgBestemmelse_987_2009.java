package no.nav.melosys.domain.bestemmelse;

public enum LovvalgBestemmelse_987_2009 implements LovvalgBestemmelse {

    ART14_11("FO_987_2009_ART14_11", "Arbeidstaker i flere to/flere land -forreningsted utenfor EØS område");

    private String kode;
    private String beskrivelse;

    LovvalgBestemmelse_987_2009(String kode, String beskrivelse) {
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
