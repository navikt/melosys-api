package no.nav.melosys.service.oppgave;

public enum OppgaveBehandlingstema {
    EU_EOS_LAND("ab0424"),
    AVTALELAND("ab0387"),
    UTENFOR_AVTALELAND("ab0388"),
    PENSJONIST_ELLER_UFORETRYGDET("ab0355"),
    YRKESAKTIV("ab0462"),
    ANMODNING_UNNTAK("ab0460"),
    REGISTRERING_UNNTAK("ab0461");

    private final String kode;

    OppgaveBehandlingstema(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
