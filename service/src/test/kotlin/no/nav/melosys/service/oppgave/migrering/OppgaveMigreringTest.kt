package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import java.io.File

class OppgaveMigreringTest {
    @Test
    fun test() {
        val json = File("/Users/rune/div/diff-q2.json").readText()
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val migreringsInfos: List<MigreringsInfo> =
            mapper.readValue(json, object : TypeReference<List<MigreringsInfo>>() {})

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
