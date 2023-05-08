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

        document {
            table {
                th {
                    item(bc = "BurlyWood") { "Sakstype" }
                    item(bc = "BurlyWood") { "Sakstema" }
                    item(bc = "BurlyWood") { "Behandlingstype" }
                    item(bc = "BurlyWood") { "Behandlingstema" }
                    item(bc = "Orange") { "Mapping regel" }
                    item(bc = "Orange") { "Antall" }
                    item(bc = "DarkSalmon") { "Behandlingstema" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                    item(bc = "DarkSalmon") { "Tema" }
                    item(bc = "DarkSalmon") { "Oppgavetype" }
                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                }

                migreringsRapport.sortedMigreringsListe().asSequence().filter {
                    !it.ny.fantIkkeOppgaveMapping() && it.oppgaver.size == 1
                }
                    .map { it.sak.tilSak() }.groupBy { it }.toList()
                    .map { it.first to it.second.size }
                    .sortedBy { it.second }.toList().reversed().forEach {
                        val sak = it.first
                        val oppgave: OppgaveGosysMapping.Oppgave =
                            oppgaveGosysMapping.finnOppgave(sak.sakstype, sak.sakstema, sak.behandlingstema, sak.behandlingstype)
                        val fargeFraRegelTuffet = when(oppgave.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                            OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                        }
                        tr {
                            item(bc = "OldLace") { sak.sakstype }
                            item(bc = "OldLace") { sak.sakstema }
                            item(bc = "OldLace") { sak.behandlingstype }
                            item(bc = "OldLace") { sak.behandlingstema }
                            item(bc = fargeFraRegelTuffet) { oppgave.regelTruffet.beskrivelse }
                            item(bc = "PaleGoldenRod") { it.second }
                            item(bc = "PeachPuff") { oppgave.oppgaveBehandlingstema?.kode }
                            item(bc = "PeachPuff") { oppgave.oppgaveBehandlingstema?.name }
                            item(bc = "PeachPuff") { oppgave.tema }
                            item(bc = "PeachPuff") { oppgave.oppgaveType }
                            item(bc = "PeachPuff") { oppgave.beskrivelsefelt }

                        }
                    }
                }
        }.apply {
            export(HtmlExporter()).let {
                File("/Users/rune/div/filtert-mot-migrering.html").writeText(it)
            }
            export(ConfluenceWikiExporter()).let {
                File("/Users/rune/div/filtert-mot-migrering.txt").writeText(it)
            }
        }
    }

    @Test
    fun `gyldige melosys kombinasjoner som tabell`() {
        document {
            table {
                th {
                    item(bc = "BurlyWood") { "Sakstype" }
                    item(bc = "BurlyWood") { "Sakstema" }
                    item(bc = "BurlyWood") { "Behandlingstype" }
                    item(bc = "BurlyWood") { "Behandlingstema" }
                    item(bc = "Chocolate") { "Mapping regel" }
                    item(bc = "DarkSalmon") { "Behandlingstema" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                    item(bc = "DarkSalmon") { "Tema" }
                    item(bc = "DarkSalmon") { "Oppgavetype" }
                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                }
                oppgaveGosysMapping.all().sortedWith(
                    compareBy(
                        { it.sakstype },
                        { it.sakstema },
                        { it.behandlingstype },
                        { it.behandlingstema }
                    )
                ).forEach {
                    val oppgave = oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
                    val fargeFraRegelTuffet = when(oppgave.regelTruffet) {
                        OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                        OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                        OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                    }
                    val fontType = when(oppgave.regelTruffet) {
                        OppgaveGosysMapping.Regel.FRA_TABELL -> Font.NORMAL
                        OppgaveGosysMapping.Regel.HENVENDELSE -> Font.ITALIC
                        OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> Font.STRONG
                    }
                    tr {
                        item(bc = "OldLace") { it.sakstype }
                        item(bc = "OldLace") { it.sakstema }
                        item(bc = "OldLace") { it.behandlingstype }
                        item(bc = "OldLace") { it.behandlingstema }
                        item(bc = fargeFraRegelTuffet, font = fontType) { oppgave.regelTruffet.beskrivelse }
                        item(bc = "PeachPuff") { oppgave.oppgaveBehandlingstema?.kode }
                        item(bc = "PeachPuff") { oppgave.oppgaveBehandlingstema?.name }
                        item(bc = "PeachPuff") { oppgave.tema }
                        item(bc = "PeachPuff") { oppgave.oppgaveType }
                        item(bc = "PeachPuff") { oppgave.beskrivelsefelt }
                    }
                }
            }
        }.apply {
            export(HtmlExporter()).let {
                File("/Users/rune/div/oppgave-mapping-kombinasjoner.html").writeText(it)
            }
            export(ConfluenceWikiExporter()).let {
                File("/Users/rune/div/oppgave-mapping-kombinasjoner.txt").writeText(it)
            }
        }
    }
}
