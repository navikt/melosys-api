package no.nav.melosys.integrasjon.eessi.dto;


import no.nav.melosys.domain.dokument.soeknad.UtenlandskIdent;

public class Ident {
    private String ident;
    private String landkode;

    public UtenlandskIdent tilUtenlandskIdent() {
        UtenlandskIdent utenlandskIdent = new UtenlandskIdent();
        utenlandskIdent.ident = ident;
        utenlandskIdent.landkode = landkode;
        return utenlandskIdent;
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String landkode) {
        this.landkode = landkode;
    }
}
