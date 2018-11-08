package no.nav.melosys.domain.bestemmelse;

public enum LovvalgBestemmelse_987_2009 implements LovvalgBestemmelse {

    FO_987_2009_ART14_11("Arbeidstaker i flere to/flere land -forreningsted utenfor EØS område");

    private String beskrivelse;

    LovvalgBestemmelse_987_2009(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return name();
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
