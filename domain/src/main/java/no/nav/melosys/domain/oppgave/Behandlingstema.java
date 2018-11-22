package no.nav.melosys.domain.oppgave;

import no.nav.melosys.domain.Kodeverk;

public enum Behandlingstema implements Kodeverk {
    ARB_AA("ab0387", "Arbeidstakere annen avtale"),
    ARB_FOLK("ab0388", "Arbeidstakere folketrygdloven"),
    ARB_EØS("ab0390", "Arbeidstakere innen EØS");

    private final String kode;
    private final String beskrivelse;

    Behandlingstema(String kode, String beskrivelse) {
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
