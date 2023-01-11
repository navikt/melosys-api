package no.nav.melosys.tjenester.gui.dto.brev;

public enum BrevmalFeltKode {
    BREV_TITTEL("Overskrift i brev"),
    DISTRIBUSJONSTYPE("Type brev"),
    DOKUMENTTITTEL("Dokumenttittel (valgfritt)"),
    FRITEKST("Tekst til brev"),
    FRITEKSTVEDLEGG("Legg til fritekstvedlegg"),
    INNLEDNING_FRITEKST("Innledningstekst"),
    MANGLER_FRITEKST("Hva skal mottakeren sende inn?"),
    STANDARDTEKST_KONTAKTINFORMASJON("Legg til standardtekst med kontaktinformasjon nederst i brevet"),
    VEDLEGG("Vedlegg");

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
