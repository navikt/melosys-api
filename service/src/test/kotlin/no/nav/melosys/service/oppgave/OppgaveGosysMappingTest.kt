package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.io.File

class oppgaveGosysMappingTest {

    @Test
    fun lagMappingFraConfluenseTabell() {
        println("val tableRows = listOf(")
        parseCsvFile("oppgave-gosys-mapping.csv").forEach { fields ->
            val sakstype = Sakstyper.valueOf(
                fields[0]
                    .replace("/", "_")
                    .replace("Ø", "O")
            )

            val sakstema = Sakstemaer.valueOf(fields[1])
            val behandlingstyper = fields[2].split("\n").map { Behandlingstyper.valueOf(it) }.toList()
            val behandlingstema = fields[3].split("\n").map { Behandlingstema.valueOf(it) }.toList()
            val oppgaveBehandlingstema: OppgaveBehandlingstema =
                OppgaveBehandlingstema.values().find { it.kode == fields[4] }!!
            val tema = Tema.valueOf(fields[6])
            val oppgavetyper = with(fields[7]) {
                when {
                    contains("BEH_SAK_MK") -> Oppgavetyper.BEH_SAK_MK
                    contains("Behandle SED") -> Oppgavetyper.BEH_SED
                    else -> throw IllegalStateException("Fant ikke ${fields[7]}")
                }
            }
            val beskrivelsefelt = with(fields[8]) {
                when {
                    contains("tomt") -> OppgaveGosysMapping.Beskrivelsefelt.TOMT
                    contains("SED") -> OppgaveGosysMapping.Beskrivelsefelt.SED
                    equals("A1_ANMODNING_OM_UNNTAK_PAPIR") -> OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
                    else -> throw IllegalStateException("Fant ikke ${fields[8]}")
                }
            }

            val row = "    TableRow(\n" +
                "        Sakstyper.${sakstype},\n" +
                "        Sakstemaer.${sakstema},\n" +
                "        setOf(${behandlingstyper.joinToString { "Behandlingstyper.$it" }}),\n" +
                "        setOf(${behandlingstema.joinToString { "Behandlingstema.$it" }}),\n" +
                "        oppgaveGosysMapping.Oppgave(OppgaveBehandlingstema.$oppgaveBehandlingstema, Tema.$tema, Oppgavetyper.$oppgavetyper, Beskrivelsefelt.$beskrivelsefelt)\n" +
                "    ),"
            println(row)
        }
    }

    // Lag at av chat GPT-4
    fun parseCsvFile(filePath: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        File(filePath).bufferedReader().use { reader ->
            // Skip header line
            reader.readLine()
            reader.readLine()

            val row = mutableListOf<String>()
            var cell = StringBuilder()
            var insideQuotes = false

            reader.forEachLine { line ->
                line.forEachIndexed { index, c ->
                    when (c) {
                        '"' -> insideQuotes = !insideQuotes
                        ';' -> {
                            if (insideQuotes) {
                                cell.append(c)
                            } else {
                                row.add(cell.toString())
                                cell = StringBuilder()

                                if (row.size == 9) {
                                    rows.add(row.toList())
                                    row.clear()
                                }
                            }
                        }

                        '\n' -> {
                            if (insideQuotes) {
                                cell.append(c)
                            }
                        }

                        else -> cell.append(c)
                    }
                }
                if (insideQuotes) {
                    cell.append('\n')
                } else {
                    row.add(cell.toString())
                    rows.add(row.toList())
                    row.clear()
                    cell = StringBuilder()
                }
            }
        }
        return rows
    }
}
