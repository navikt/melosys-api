package no.nav.melosys.tjenester.gui.dto.brev;

public enum BrevmalFeltKode {
    BREV_TITTEL("Overskrift i brev"),
    DISTRIBUSJONSTYPE("Type brev"),
    FRITEKST("Tekst til brev"),
    FRITEKSTVEDLEGG("Legg til fritekstvedlegg"),
    MANGLER_FRITEKST("Hva skal mottakeren sende inn?"),
    DOKUMENT_TITTEL("Dokumenttittel (valgfritt)");
    INNLEDNING_FRITEKST("Innledningstekst"),
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
