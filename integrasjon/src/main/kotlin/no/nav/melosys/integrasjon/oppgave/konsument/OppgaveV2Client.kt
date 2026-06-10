package no.nav.melosys.integrasjon.oppgave.konsument

import tools.jackson.databind.JsonNode
import no.nav.melosys.exception.TekniskException
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient

/**
 * Klient mot Oppgave-API v2 (beta). Brukes kun for nøkkelord, som ikke finnes i v1.
 * Oppgave-id-ene er de samme i v1 og v2, så nøkkelord kan settes på oppgaver opprettet via v1.
 */
@Retryable
open class OppgaveV2Client(private val webClient: WebClient) {

    open fun leggTilNøkkelord(oppgaveID: String, nøkkelord: Set<String>) {
        val oppgave = hentOppgave(oppgaveID)
        // NB: JsonNode.map er en medlemsmetode i Jackson 3 som mapper noden selv,
        // ikke elementene — derfor values() før Kotlin-map.
        val eksisterendeNøkkelord: Set<String> = oppgave.path("nokkelord").values().map { it.asString() }.toSet()
        patchNøkkelord(oppgaveID, oppgave, eksisterendeNøkkelord + nøkkelord)
    }

    private fun hentOppgave(oppgaveID: String): JsonNode =
        webClient.get()
            .uri(OPPGAVE_URI_MED_ID, oppgaveID)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()
            ?: throw TekniskException("Tomt svar fra Oppgave v2 ved henting av oppgave $oppgaveID")

    // PATCH er merge-patch som erstatter hele nøkkelord-listen, og skjemaet krever
    // aktivDato, fordeling, kategorisering, prioritet og status — disse ekkoes fra GET-responsen.
    private fun patchNøkkelord(oppgaveID: String, oppgave: JsonNode, nøkkelord: Set<String>) {
        webClient.patch()
            .uri(OPPGAVE_URI_MED_ID, oppgaveID)
            .bodyValue(
                mapOf(
                    "aktivDato" to oppgave["aktivDato"],
                    "fordeling" to oppgave["fordeling"],
                    "kategorisering" to oppgave["kategorisering"],
                    "prioritet" to oppgave["prioritet"],
                    "status" to oppgave["status"],
                    "nokkelord" to nøkkelord
                )
            )
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    companion object {
        private const val OPPGAVE_URI_MED_ID = "/oppgaver/{oppgaveID}"
    }
}
