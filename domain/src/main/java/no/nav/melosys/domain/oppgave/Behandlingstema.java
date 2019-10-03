package no.nav.melosys.domain.oppgave;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum Behandlingstema implements Kodeverk {
    FTRL("ab0387", "Arbeidstakere annen avtale"),
    TRYGDEAVTALE("ab0388", "Arbeidstakere folketrygdloven"),
    EU_EOS("ab0424", "Arbeidstakere innen EØS");

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
