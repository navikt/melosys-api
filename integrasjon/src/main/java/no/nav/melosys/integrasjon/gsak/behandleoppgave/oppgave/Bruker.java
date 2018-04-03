package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave;

import no.nav.melosys.domain.gsak.AktorType;

public class Bruker {
    private AktorType aktørType;
    private String ident;

    public Bruker(AktorType aktørType, String ident) {
        this.aktørType = aktørType;
        this.ident = ident;
    }

    public AktorType getAktørType() {
        return aktørType;
    }

    public void setAktørType(AktorType aktørType) {
        this.aktørType = aktørType;
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }
}
