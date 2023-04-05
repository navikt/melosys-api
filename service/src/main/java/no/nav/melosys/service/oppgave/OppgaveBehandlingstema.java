package no.nav.melosys.service.oppgave;

public enum OppgaveBehandlingstema {
    EU_EOS_LAND("ab0424"),
    AVTALELAND("ab0387"),
    UTENFOR_AVTALELAND("ab0388"),
    PENSJONIST_ELLER_UFORETRYGDET("ab0355"),
    YRKESAKTIV("ab0462"),
    ANMODNING_UNNTAK("ab0460"),
    REGISTRERING_UNNTAK("ab0461"),
    AB0483("ab0483"),
    AB0480("ab0480"),
    AB0482("ab0482"),
    AB0479("ab0479"),
    AB0484("ab0484"),
    AB0485("ab0485"),
    AB0486("ab0486"),
    AB0475("ab0475"),
    AB0476("ab0476"),
    AB0477("ab0477"),
    AB0493("ab0493"),
    AB0490("ab0490"),
    AB0491("ab0491"),
    AB0492("ab0492"),
    AB0478("ab0478"),
    AB0488("ab0488"),
    AB0489("ab0489");
    private final String kode;

    OppgaveBehandlingstema(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
