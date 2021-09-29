package no.nav.melosys.service.tilgang;

public interface Aksesskontroll {

    void autoriserSakstilgang(String saksnummer);
    void autoriser(long behandlingID);
    void autoriser(long behandlingID, Aksesstype aksesstype);
    void autoriserSkrivTilRessurs(long behandlingID, Ressurs ressurs);
}
