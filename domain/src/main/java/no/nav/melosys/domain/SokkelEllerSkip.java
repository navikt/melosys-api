package no.nav.melosys.domain;

public enum SokkelEllerSkip implements Kodeverk {

    SOKKEL("SOKKEL ", "Arbeider på sokkel"),
    SKIP("SKIP", "Arbeider på skip"),
    IKKE("IKKE", "Arbeider ikke på sokkel eller skip");

    private final String kode;
    private final String beskrivelse;

    SokkelEllerSkip(String kode, String beskrivelse) {
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
