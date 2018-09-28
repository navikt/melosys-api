package no.nav.melosys.domain.bestemmelse;

public enum TilleggBestemmelse implements LovvalgBestemmelse {

    ART11_2("ART11_2", "Mottar kontantytelse"),
    ART11_4_1("ART11_4_1 ", "Arbeid på skip"),
    ART11_5("ART11_5", "Arbeid som flyende personell"),
    ART13_3("ART13_3", "Arbeidstaker og selvstendig virksomhet i to/flere land"),
    ART13_4("ART13_4", "Tjenestemann og arbeidstaker/ selvstendig virksomhet i to/flere land ");

    private String kode;
    private String beskrivelse;

    TilleggBestemmelse(String kode, String beskrivelse) {
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
