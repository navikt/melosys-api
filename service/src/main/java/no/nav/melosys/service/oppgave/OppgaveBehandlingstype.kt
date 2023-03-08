package no.nav.melosys.service.oppgave;

public enum OppgaveBehandlingstype {
    EOS_LOVVALG_NORGE("ae0112");

    private final String kode;

    OppgaveBehandlingstype(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
