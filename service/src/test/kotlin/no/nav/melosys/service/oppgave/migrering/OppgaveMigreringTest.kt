package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.melosys.domain.SakOgBehandlingDTO
import org.junit.jupiter.api.Test
import java.io.File

class OppgaveMigreringTest {
    data class MigreringsInfoForLesing(
        val sak: SakOgBehandlingDTO,
        val oppgaver: List<MigreringsOppgave>,
        val ny: OppgaveOppdatering,
    ) {
        fun tilMigreringsInfo(): MigreringsInfo = MigreringsInfo(sak, oppgaver.map { it.tilOppgave() }, ny)
    }

    @Test
    fun test() {
        val json = File("/Users/rune/div/diff-q2.json").readText()
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val lesJson: List<MigreringsInfoForLesing> =
            mapper.readValue(json, object : TypeReference<List<MigreringsInfoForLesing>>() {})

        val migreringsInfos: List<MigreringsInfo> =
            lesJson.map { it.tilMigreringsInfo() }

        val joinToString = migreringsInfos
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
                    $joinToString
                   </table>
                </body>
            </html>
            """
        File("/Users/rune/div/diff.html").writeText(html)
    }
}
