package no.nav.melosys.service.oppgave.migrering

import no.finn.unleash.FakeUnleash
import no.nav.melosys.service.oppgave.OppgaveGosysMapping
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled("brukes bare for å lage oppgave migrering html rapporter, fjerns fra git etter at migreing er utført")
class LagRapportTest {
    private val oppgaveGosysMapping = OppgaveGosysMapping(FakeUnleash())

    @Test
    fun `lag rapport hvor kombinasjoner er gruppert etter antall pr kombinasjon`() {
        val oppgaveGosysMapping = OppgaveGosysMapping(FakeUnleash().apply { enable(OppgaveGosysMapping.NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING) })
        val migreringsRapport = Migrering.migreringsRapportFraJson("/Users/rune/div/jsonrapport-prod.json")

        val tableRows = migreringsRapport.sortedMigreringsListe().asSequence().filter {
            !it.ny.fantIkkeOppgaveMapping() &&
                it.oppgaver.size == 1
        }
            .map { it.sak.tilSak() }.groupBy { it }.toList()
            .map { it.first to it.second.size }
            .sortedBy { it.second }.toList().reversed().apply {
                println("size:$size")
            }.joinToString("\n") {
                val dto = it.first
                val oppgave: OppgaveGosysMapping.Oppgave =
                    oppgaveGosysMapping.finnOppgave(dto.sakstype, dto.sakstema, dto.behandlingstema, dto.behandlingstype)
                """<tr>
                    ${dto.tilTableRow()}
                    <td>${it.second}</td>
                    ${oppgave.tilTableRow()}
                </tr>
            """.trimMargin()
            }
        val html = tilHtml(tableRows)

        File("/Users/rune/div/filtert-mot-migrering.html").writeText(html)
    }

    @Test
    fun `mapping regler gruppert`() {
        oppgaveGosysMapping.allGrouped { name, list : List<Migrering.TableRowSingle> ->
            println("${name} - ${list.size}")
            val tableRows = list.sortedWith(
                compareBy(
                    { it.sakstype },
                    { it.sakstema },
                    { it.behandlingstype },
                    { it.behandlingstema }
                )
            ).joinToString("\n") {
                val oppgave: OppgaveGosysMapping.Oppgave =
                    oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
                """<tr>
                    ${it.tilTableRow()}
                    ${oppgave.tilTableRow()}
                </tr>
            """.trimMargin()
            }

            File("/Users/rune/div/regler-fra-$name.html").writeText(tilHtml(tableRows))
        }
    }

    @Test
    fun `gyldige melosys kombinasjoner som tabell`() {
        val tableRows = oppgaveGosysMapping.all().apply { println("all size:$size") }.sortedWith(
            compareBy(
                { it.sakstype },
                { it.sakstema },
                { it.behandlingstype },
                { it.behandlingstema }
            )
        ).apply { println("fitered size:$size") }.joinToString("\n") {
            val oppgave: OppgaveGosysMapping.Oppgave =
                oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
            """<tr>
                    ${it.tilTableRow()}
                    ${oppgave.tilTableRow()}
                </tr>
            """.trimMargin()
        }

        val html = tilHtml(tableRows)

        File("/Users/rune/div/gyldige-kobos3.html").writeText(html)
    }

    private fun tilHtml(tableRows: String): String {
        val html = """
                <!DOCTYPE html>
                <html>
                <style>
                    table, th, td {
                        border: 1px solid black;
                        border-collapse: collapse;
                    }
                    th, td {
                        padding: 2px;
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
                        $tableRows
                       </table>
                    </body>
                </html>
                """
        return html
    }

    private fun OppgaveGosysMapping.Oppgave.tilTableRow(): String {
        val sakStyle = "class=\"oppgave\""
        return """
            <td $sakStyle> ${oppgaveBehandlingstema?.kode}</td>
            <td $sakStyle> ${oppgaveBehandlingstema?.name}</td>
            <td $sakStyle> ${tema.name}</td>
            <td $sakStyle> ${oppgaveType.kode}</td>
            <td $sakStyle> ${beskrivelsefelt.name}</td>
        """.trimIndent()

    }

    private fun Migrering.Sak.tilTableRow(): String {
        val sakStyle = "class=\"sak\""
        return """
            <td $sakStyle> $sakstype</td>
            <td $sakStyle> ${splitLong(sakstema.name)}</td>
            <td $sakStyle> ${splitLong(behandlingstype.kode)}</td>
            <td $sakStyle> ${splitLong(behandlingstema.kode)}</td>
        """.trimIndent()
    }

    private fun Migrering.TableRowSingle.tilTableRow(): String {
        val sakStyle = "class=\"sak\""
        return """
            <td $sakStyle> $sakstype</td>
            <td $sakStyle> ${splitLong(sakstema.name)}</td>
            <td $sakStyle> ${splitLong(behandlingstype.kode)}</td>
            <td $sakStyle> ${splitLong(behandlingstema.kode)}</td>
        """.trimIndent()
    }

    private fun splitLong(name: String, max: Int = 100): String {
        if (name.length > max)
            return name.split("_").joinToString("</br>")
        return name
    }
}
