package no.nav.melosys.service.oppgave

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
        val diffs: List<OppgaveMigrering.Diff> =
            mapper.readValue(json, object : TypeReference<List<OppgaveMigrering.Diff>>() {})

        val joinToString = diffs
            .sortedWith(
                compareBy(
                    { it.sak.sakstype },
                    { it.sak.sakstema },
                    { it.sak.behandlingstype },
                    { it.sak.behandlingstema },
                    { it.sak.behandlingstatus },
                )
            )
//            .sortedBy { it.sak.sakstype }
            .joinToString("") { it.htmlTableRow() }

        val html = """
            <!DOCTYPE html>
            <html>
                <body>
                   <table border="1">
                   <tr border="1">
                    <td colspan=3>hei</td>
                   </tr>
                    $joinToString
                   </table>
                </body>
            </html>
            """
        File("/Users/rune/div/diff.html").writeText(html)
    }
}
