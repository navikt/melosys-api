package no.nav.melosys.tjenester.gui.dto.brev;

public enum BrevmalFeltKode {
    BREV_TITTEL("Overskrift i brev"),
    DISTRIBUSJONSTYPE("Type brev"),
    FRITEKST("Hovedtekst til brev"),
    FRITEKSTVEDLEGG("Legg til fritekstvedlegg"),
    STANDARDTEKST_INNHENTINGBREVFORMTITTEL("Hva skal mottakeren sende inn?"),
    MANGLER_FRITEKST("Hva skal mottakeren sende inn?"),
    DOKUMENT_TITTEL("Dokumenttittel (valgfritt)"),
    INNLEDNING_FRITEKST("Innledningstekst"),
    STANDARDTEKST_KONTAKTINFORMASJON("Legg til standardtekst med kontaktinformasjon nederst i brevet"),
    STANDARDTEKST("Standardtekst søknad/klage"),
    STANDARDTEKST_INNTEKTSOPPLYSNINGER("Standardtekst for opplysninger om inntekt"),
    VEDLEGG("Vedlegg"),
    UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER("Trygdemyndighet");

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
