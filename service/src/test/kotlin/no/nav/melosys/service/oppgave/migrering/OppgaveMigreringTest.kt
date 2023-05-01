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
        val migreringsSaker: List<MigreringsSak> = hentMigreringsSaker("/Users/rune/div/diff-q2.json")

        val migreringsSakerHtml = migreringsSaker
            .sortedWith(
                compareBy(
                    { it.sak.sakstype },
                    { it.sak.sakstema },
                    { it.sak.behandlingstype },
                    { it.sak.behandlingstema },
                    { it.sak.behandlingstatus },
                )
            )
            .filter { it.harFeil() || it.teamErForsjellig() || it.oppgavetypeErForsjellig() }
            .filter { it.oppgaver.isNotEmpty() }
            .joinToString("") { it.htmlTableRow() }

        val html = """
            <!DOCTYPE html>
            <html>
            <style>
                table, th, td {
                    border: 1px solid black;
                    border-collapse: collapse;
                }
                .sak {
                    background-color:#fff0b3
                }
                .ny {
                    background-color:#abf5d1
                }
                .oppgave {
                    background-color:#ffebe6
                }
                .feil {
                    background-color: #ff5656
                }
                .sakfeil {
                    background-color: #ffceb3
                }
                .forskjell {
                    background-color: rgba(255, 33, 0, 0.61)
                }
            </style>
                <body>
                   <table>
                        <tr>
                            <th class="sak" colspan=6>melosys sak</th>
                            <th class="ny" colspan=4>migrert oppgave</th>
                            <th class="oppgave" colspan=8>orginal oppgave</th>
                        </tr>
                        <tr>
                            <th class="sak">sakstype</th>
                            <th class="sak">sakstema</th>
                            <th class="sak">behandlingstype</th>
                            <th class="sak">behandlingstema</th>
                            <th class="sak">behandlingstatus</th>
                            <th class="sak">saksnummer</th>
                            <th class="ny">behandlings<br>tema</th>
                            <th class="ny">tema</th>
                            <th class="ny">oppgavetype</th>
                            <th class="ny">beskrivelse</th>
                            <th class="oppgave">behandlings<br>tema</th>
                            <th class="oppgave">tema</th>
                            <th class="oppgave">oppgavetype</th>
                            <th class="oppgave">behandlings<br>type</th>
                            <th class="oppgave">beskrivelse</th>
                            <th class="oppgave">bruker</th>
                            <th class="oppgave">opprettet tidspunkt</th>
                            <th class="oppgave">frist</th>
                        </tr>
                    $migreringsSakerHtml
                   </table>
                </body>
            </html>
            """

        File("/Users/rune/div/diff.html").writeText(html)
    }

    private fun hentMigreringsSaker(fileName: String): List<MigreringsSak> {
        return jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .readValue(File(fileName), object : TypeReference<List<MigreringsInfoForLesing>>() {})
            .map { it.tilMigreringsInfo() }
    }

    private data class MigreringsInfoForLesing(
        val sak: SakOgBehandlingDTO, val oppgaver: List<MigreringsOppgave>, val ny: OppgaveOppdatering,
    ) {
        fun tilMigreringsInfo(): MigreringsSak = MigreringsSak(sak, oppgaver.map { it.tilOppgave() }, ny)

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
