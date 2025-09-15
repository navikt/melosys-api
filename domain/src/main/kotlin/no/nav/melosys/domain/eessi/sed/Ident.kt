package no.nav.melosys.domain.eessi.sed;


import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent;

public class Ident {
    private String ident;
    private String landkode;

    public UtenlandskIdent tilUtenlandskIdent() {
        UtenlandskIdent utenlandskIdent = new UtenlandskIdent();
        utenlandskIdent.setIdent(ident);
        utenlandskIdent.setLandkode(landkode);
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
