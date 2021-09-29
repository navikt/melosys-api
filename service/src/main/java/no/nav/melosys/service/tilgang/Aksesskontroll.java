package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Fagsak;

public interface Aksesskontroll {

    void autoriserSakstilgang(String saksnummer);
    void autoriserSakstilgang(Fagsak fagsak);
    void autoriser(long behandlingID);
    void autoriser(long behandlingID, Aksesstype aksesstype);
    void autoriserSkriv(long behandlingID);
    void autoriserSkrivOgTilordnet(long behandlingID);
    void autoriserSkrivTilRessurs(long behandlingID, Ressurs ressurs);
    void autoriserFolkeregisterIdent(String brukerID);
}
