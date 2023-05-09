package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled("brukes bare for å lage oppgave migrering rapporter, fjerns fra git etter at migreing er utført")
class GyldigeKombinasjonerTest {

    @Test
    fun `lag tabell med alle gyldige kombinasjoner av sakstype, sakstema, behandlingstype, behandlingstema`() {
        document {
            table {
                th {
                    item { "Sakstype" }
                    item { "Sakstema" }
                    item { "Behandlingstype" }
                    item { "Behandlingstema" }
                    item { "Mapping regel" }
                    GyldigeKombinasjoner.rowsMelosysOgDatavarehus.sortedWith(
                        compareBy(
                            { it.sakstype },
                            { it.sakstema },
                            { it.behandlingstype },
                            { it.behandlingstema }
                        )
                    ).forEach { sak ->
                        val fontType = when (sak.regel) {
                            GyldigeKombinasjoner.Regel.MELOSYS -> Font.STRONG
                            GyldigeKombinasjoner.Regel.DVH -> Font.STRONG
                            GyldigeKombinasjoner.Regel.Begge -> Font.NORMAL
                        }
                        tr {
                            item(font = fontType) { sak.sakstype }
                            item(font = fontType) { sak.sakstema }
                            item(font = fontType) { sak.behandlingstype }
                            item(font = fontType) { sak.behandlingstema }
                            item(font = fontType) { sak.regel.beskrivelse }
                        }
                    }
                }
            }
        }.run {
            export(HtmlExporter()).let {
                File("/Users/rune/div/gyldige/gyldige.html").writeText(it)
            }
            export(ConfluenceWikiExporter()).let {
                File("/Users/rune/div/gyldige/gyldige.txt").writeText(it)
            }
        }
    }
}
