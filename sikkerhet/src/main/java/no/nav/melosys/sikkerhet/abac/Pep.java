package no.nav.melosys.sikkerhet.abac;

public interface Pep {
    void sjekkTilgangTilFnr(String fnr);
    void sjekkTilgangTilAktoerId(String aktoerId);
}
