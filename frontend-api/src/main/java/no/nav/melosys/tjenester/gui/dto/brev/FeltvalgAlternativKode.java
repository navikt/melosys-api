package no.nav.melosys.tjenester.gui.dto.brev;

public enum FeltvalgAlternativKode {

    HENVENDELSE_OM_TRYGDETILHØRLIGHET("Svar på henvendelse om trygdetilhørlighet"),
    ORIENTERING_BESLUTNING("Orientering om vår beslutning"),
    CONFIRMATION_OF_MEMBERSHIP("Confirmation of membership in the National Insurance Scheme"),
    BEKREFTELSE_PÅ_MEDLEMSKAP("Bekreftelse på medlemskap i folketrygden"),
    HENVENDELSE_OM_MEDLEMSKAP("Svar på henvendelse om medlemskap i folketrygden"),
    FRITEKST("Fritekst"),
    ENGELSK_FRITEKSTBREV("Request to remain subject to Norwegian legislation"),
    STANDARD("Standardtekst søknad/klage"),
    ETAT_SKATTEETATEN("Skatteetaten"),
    ETAT_SKATTEINNKREVER_UTLAND("Skatteinnkrever utland"),
    ETAT_HELFO("Helfo");

    private final String beskrivelse;

    FeltvalgAlternativKode(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return name();
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
