package no.nav.melosys.service.tilgang;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;

public interface Aksesskontroll {

    void auditAutoriserFolkeregisterIdent(String ident, String kontekst);
    void auditAutoriserSakstilgang(Fagsak fagsak, String kontekst);
    void auditAutoriserSakstilgang(String saksnummer, String kontekst);

    void autoriserSakstilgang(String saksnummer);
    void autoriserSakstilgang(Fagsak fagsak);
    void autoriser(long behandlingID);
    void autoriser(long behandlingID, Aksesstype aksesstype);
    void autoriserSkriv(long behandlingID);
    void autoriserSkrivOgTilordnet(long behandlingID);
    void autoriserSkrivTilRessurs(long behandlingID, Ressurs ressurs);
    void autoriserFolkeregisterIdent(String brukerID);
    boolean behandlingKanRedigeresAvSaksbehandler(Behandling behandling, String saksbehandler);
    boolean behandlingKanRedigeresAvSaksbehandler(long behandlingID);
}
