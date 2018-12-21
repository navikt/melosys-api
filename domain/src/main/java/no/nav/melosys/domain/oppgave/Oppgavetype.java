package no.nav.melosys.domain.oppgave;

import no.nav.melosys.domain.Kodeverk;

public enum Oppgavetype implements Kodeverk {
    BEH_SAK("BEH_SAK"), // Saksbehandling
    JFR("JFR"); //Journalføring

    private final String kode;

    Oppgavetype(String kode) {
        this.kode = kode;
    }

    @Override
    public String getKode() {
        return kode;
    }
}
