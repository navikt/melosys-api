package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.PrioritetType
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime

class OppgaveMigreringTest {

    @Test
    fun test() {
        val migreringsSaker: List<MigreringsInfo> = hentMigreringsSaker("/Users/rune/div/diff-q2.json")

        val migreringsSakerHtml = migreringsSaker
            .sortedWith(
                compareBy(
                    { it.sak.sakstype },
                    { it.sak.sakstema },
                    { it.sak.behandlingstype },
                    { it.sak.behandlingstema },
                    { it.sak.behandlingstatus },
                )
            ).filter { it.harFeil() }
            .filter { it.oppgaver.isNotEmpty() }
            .joinToString("") { it.htmlTableRow() }

        val html = """
            <!DOCTYPE html>
            <html>
                <body>
                   <table border="1">
                    $migreringsSakerHtml
                   </table>
                </body>
            </html>
            """

        File("/Users/rune/div/diff.html").writeText(html)
    }

    private fun hentMigreringsSaker(fileName: String): List<MigreringsInfo> {
        val json = File(fileName).readText()
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val migreringsInfos: List<MigreringsInfo> =
            mapper.readValue(json, object : TypeReference<List<MigreringsInfoForLesing>>() {})
                .map { it.tilMigreringsInfo() }
        return migreringsInfos
    }

    data class MigreringsInfoForLesing(
        val sak: SakOgBehandlingDTO, val oppgaver: List<MigreringsOppgave>, val ny: OppgaveOppdatering,
    ) {
        fun tilMigreringsInfo(): MigreringsInfo = MigreringsInfo(sak, oppgaver.map { it.tilOppgave() }, ny)

        data class MigreringsOppgave(
            val aktørId: String? = null,
            val orgnr: String? = null,
            val behandlingstema: String? = null,
            val behandlingstype: String? = null,
            val beskrivelse: String? = null,
            val behandlesAvApplikasjon: Fagsystem? = null,
            val opprettetTidspunkt: ZonedDateTime? = null,
            val fristFerdigstillelse: LocalDate? = null,
            val journalpostId: String? = null,
            val oppgaveId: String? = null,
            val oppgavetype: Oppgavetyper? = null,
            val prioritet: PrioritetType? = null,
            val saksnummer: String? = null,
            val tema: Tema? = null,
            val temagruppe: String? = null,
            val tilordnetRessurs: String? = null,
            val tildeltEnhetsnr: String? = null,
            val versjon: Int? = null,
            val aktivDato: LocalDate? = null,
            val status: String? = null
        ) {
            fun tilOppgave(): Oppgave = Oppgave.Builder()
                .setOppgaveId(oppgaveId)
                .setBehandlesAvApplikasjon(behandlesAvApplikasjon)
                .setSaksnummer(saksnummer)
                .setBeskrivelse(beskrivelse)
                .setOpprettetTidspunkt(opprettetTidspunkt)
                .setFristFerdigstillelse(fristFerdigstillelse)
                .setTema(tema)
                .setOppgavetype(oppgavetype)
                .setPrioritet(prioritet)
                .setJournalpostId(journalpostId)
                .setTilordnetRessurs(tilordnetRessurs)
                .setVersjon(versjon!!)
                .setAktørId(aktørId)
                .setOrgnr(orgnr)
                .setBehandlingstema(behandlingstema)
                .setBehandlingstype(behandlingstype)
                .setTemagruppe(temagruppe)
                .setTildeltEnhetsnr(tildeltEnhetsnr)
                .setAktivDato(aktivDato)
                .setStatus(status)
                .build()
        }
    }
}
