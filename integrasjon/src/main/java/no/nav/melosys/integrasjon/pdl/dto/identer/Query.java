package no.nav.melosys.integrasjon.pdl.dto.identer;

public final class Query {
    public static final String HENT_IDENTER_QUERY = """
            query($ident: ID!) {
              hentIdenter(ident: $ident) {
                  identer {
                      ident,
                      gruppe
                  }
              }
            }
            """;

    private Query() {
        throw new UnsupportedOperationException();
    }
}
