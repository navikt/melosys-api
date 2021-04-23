package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.util.Map;

import no.nav.melosys.domain.Fagsystem;

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

    public static Map<String, Object> dokumentoversiktVariabler(String saksnummer, String sluttpeker) {
        if (sluttpeker == null) {
            sluttpeker = "";
        }

        return Map.of(
            "fagsak", Map.of(
                "fagsakId", saksnummer,
                "fagsaksystem", Fagsystem.MELOSYS.getKode()
            ),
            "foerste", 10,
            "sluttpeker", sluttpeker
        );
    }

    public static final String dokumentoversiktQuery = """
        query($fagsak: FagsakInput!, $foerste: Int!, $etter: String) {
          query: dokumentoversiktFagsak(fagsak: $fagsak, foerste: $foerste, etter: $etter) {
            journalposter {
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
            sideInfo {
              sluttpeker
              finnesNesteSide
            }
          }
        }
        """;

    private Query() {
        throw new IllegalStateException("Utility");
    }
}
