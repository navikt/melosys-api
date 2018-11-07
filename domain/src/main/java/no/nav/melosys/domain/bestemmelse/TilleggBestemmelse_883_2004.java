package no.nav.melosys.domain.bestemmelse;

public enum TilleggBestemmelse_883_2004 implements LovvalgBestemmelse {

    FO_883_2004_ART11_2("Mottar kontantytelse"),
    FO_883_2004_ART11_4_1("Arbeid på skip"),
    FO_883_2004_ART11_5("Arbeid som flyende personell"),
    FO_883_2004_ART13_3("Arbeidstaker og selvstendig virksomhet i to/flere land"),
    FO_883_2004_ART13_4("Tjenestemann og arbeidstaker/ selvstendig virksomhet i to/flere land "),
    FO_883_2004_ART87_7("Overgangsregelen ... ?"),
    FO_883_2004_ART87A("Overgangsregelen ... ?");

    private String beskrivelse;

    TilleggBestemmelse_883_2004(String beskrivelse) {
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
