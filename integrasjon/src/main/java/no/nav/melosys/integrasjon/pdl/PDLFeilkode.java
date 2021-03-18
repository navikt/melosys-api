package no.nav.melosys.integrasjon.pdl;

abstract class PDLFeilkode {
    private PDLFeilkode() {
        throw new UnsupportedOperationException("Utility");
    }

    static final String NOT_FOUND = "not_found";
}
