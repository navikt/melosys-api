package no.nav.melosys.domain.bestemmelse;

public enum TilleggBestemmelse_883_2004 implements LovvalgBestemmelse {

    ART11_2("FO_883_2004_ART11_2", "Mottar kontantytelse"),
    ART11_4_1("FO_883_2004_ART11_4_1 ", "Arbeid på skip"),
    ART11_5("FO_883_2004_ART11_5", "Arbeid som flyende personell"),
    ART13_3("FO_883_2004_ART13_3", "Arbeidstaker og selvstendig virksomhet i to/flere land"),
    ART13_4("FO_883_2004_ART13_4", "Tjenestemann og arbeidstaker/ selvstendig virksomhet i to/flere land "),
    ART87_7("FO_883_2004_ART87_7", "Overgangsregelen ... ?"),
    ART87A("FO_883_2004_ART87A ", "Overgangsregelen ... ?");

    private String kode;
    private String beskrivelse;

    TilleggBestemmelse_883_2004(String kode, String beskrivelse) {
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
