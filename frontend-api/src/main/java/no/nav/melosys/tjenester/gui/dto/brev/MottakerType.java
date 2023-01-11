package no.nav.melosys.tjenester.gui.dto.brev;

public enum MottakerType {
    BRUKER_ELLER_BRUKERS_FULLMEKTIG ("Bruker eller brukers fullmektig"),
    VIRKSOMHET("Virksomheten saken er tilknyttet"),
    ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG("Arbeidsgiver eller arbeidsgivers fullmektig"),
    ANNEN_ORGANISASJON("Annen organisasjon"),
    UTENLANDSK_TRYGDEMYNDIGHET("Utenlandsk trygdemyndighet i avtaleland");

    MottakerType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    private final String beskrivelse;

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
