package no.nav.melosys.tjenester.gui.dto.brev;

public enum BrevmalFeltKode {
    BREV_TITTEL("Tittel"),
    DISTRIBUSJONSTYPE("Type brev"),
    STANDARDTEKST_KONTAKTINFORMASJON("Legg til standardtekst med kontaktinformasjon nederst i brevet"),
    FRITEKST("Tekst til brev"),
    INNLEDNING_FRITEKST("Innledningstekst"),
    VEDLEGG("Vedlegg"),
    FRITEKSTVEDLEGG("Legg til fritekstvedlegg"),
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
