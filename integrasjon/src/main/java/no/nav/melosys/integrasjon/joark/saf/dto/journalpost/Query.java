package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public final class Query {
    public static final String hentJournalpostQuery = """
          query($journalpostId: String!) {
            query: journalpost(journalpostId: $journalpostId) {
              journalpostId
              tittel
              journalstatus
              tema
              journalposttype
              sak {
                arkivsaksnummer
              }
              bruker {
                id
                type
              }
              avsenderMottaker {
                id
                type
                navn
              }
              kanal
              relevanteDatoer {
                dato
                datotype
              }
              dokumenter {
                dokumentInfoId
                tittel
                brevkode
                logiskeVedlegg {
                  tittel
                }
              }
            }
          }
        """;

    private Query() {
        throw new IllegalStateException("Utility");
    }
}
