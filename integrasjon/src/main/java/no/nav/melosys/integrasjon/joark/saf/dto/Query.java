package no.nav.melosys.integrasjon.joark.saf.dto;

import java.util.Map;

import no.nav.melosys.domain.Fagsystem;

public final class Query {
    private static final int HENT_ANTALL_JOURNALPOSTER = 50;
    public static final String JOURNALPOST_ID = "journalpostId";
    public static final String HENT_JOURNALPOST_QUERY = """
          query($journalpostId: String!) {
            query: journalpost(journalpostId: $journalpostId) {
              journalpostId
              tittel
              journalstatus
              tema
              journalposttype
              sak {
                fagsakId
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
                  logiskVedleggId
                  tittel
                }
                dokumentvarianter {
                  filnavn
                  filuuid
                  filtype
                  skjerming
                  saksbehandlerHarTilgang
                  variantformat
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
            "foerste", HENT_ANTALL_JOURNALPOSTER,
            "sluttpeker", sluttpeker
        );
    }

    public static final String DOKUMENTOVERSIKT_QUERY = """
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
                  logiskVedleggId
                  tittel
                }
                dokumentvarianter {
                  filnavn
                  filuuid
                  filtype
                  skjerming
                  saksbehandlerHarTilgang
                  variantformat
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
