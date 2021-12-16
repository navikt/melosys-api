package no.nav.melosys.tjenester.gui;

public enum BrevmalFeltKode {

    BREV_TITTEL("Brevtittel"),
    STANDARDTEKST_KONTAKTINFORMASJON("Legg til standardtekst med kontaktinformasjon nederst i brevet"),
    FRITEKST("Tekst til brev"),
    INNLEDNING_FRITEKST("Innledningstekst"),
    MANGLER_FRITEKST("Hva skal mottakeren sende inn?");

    private final String beskrivelse;

    BrevmalFeltKode(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return name();
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
