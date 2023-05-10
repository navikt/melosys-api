package no.nav.melosys.service.oppgave.migrering

import no.finn.unleash.FakeUnleash
import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
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
                    .map { it.sak.tilSak() }.groupBy { it }
                    .map { it.key to it.value.size }
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
        }.run {
            export(HtmlExporter()).let {
                File("/Users/rune/div/filtert-mot-migrering.html").writeText(it)
            }
            export(ConfluenceWikiExporter()).let {
                File("/Users/rune/div/filtert-mot-migrering.txt").writeText(it)
            }
        }
    }

    @Test
    fun `lag gyldige melosys kombinasjoner mappet til oppgave tabell`() {
        document {
            table {
                th {
                    item(bc = "BurlyWood") { "Sakstype" }
                    item(bc = "BurlyWood") { "Sakstema" }
                    item(bc = "BurlyWood") { "Behandlingstype" }
                    item(bc = "BurlyWood") { "Behandlingstema" }
                    item(bc = "Chocolate") { "Mapping regel" }
                    item(bc = "Chocolate") { "melosys/dvh" }
                    item(bc = "DarkSalmon") { "Behandlingstema" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                    item(bc = "DarkSalmon") { "Tema" }
                    item(bc = "DarkSalmon") { "Oppgavetype" }
                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                }
                GyldigeKombinasjoner.rowsMelosysOgDatavarehus.sortedWith(
                    compareBy(
                        { it.sakstype },
                        { it.sakstema },
                        { it.behandlingstype },
                        { it.behandlingstema }
                    )
                ).forEach {
                    val oppgave: OppgaveGosysMapping.Oppgave? = try {
                        oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
                    } catch (e: Exception) {
                        println(e.message)
                        null
                    }
                    val fargeFraRegelTuffet = when (oppgave?.regelTruffet) {
                        OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                        OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                        OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                        else -> null
                    }
                    val fontType = when (oppgave?.regelTruffet) {
                        OppgaveGosysMapping.Regel.FRA_TABELL -> Font.NORMAL
                        OppgaveGosysMapping.Regel.HENVENDELSE -> Font.ITALIC
                        OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> Font.STRONG
                        else -> Font.NORMAL
                    }
                    val maglerOppgaveFarge = if (oppgave == null) "Red" else null
                    val maglerOppgaveFont = if (oppgave == null) Font.STRONG else Font.NORMAL
                    tr {
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { it.sakstype }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { it.sakstema }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { it.behandlingstype }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { it.behandlingstema }
                        item(fc = maglerOppgaveFarge, bc = fargeFraRegelTuffet, font = fontType) { oppgave?.regelTruffet?.beskrivelse }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { it.regel.beskrivelse }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { oppgave?.oppgaveBehandlingstema?.kode }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { oppgave?.oppgaveBehandlingstema?.name }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { oppgave?.tema }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { oppgave?.oppgaveType }
                        item(fc = maglerOppgaveFarge, font = maglerOppgaveFont) { oppgave?.beskrivelsefelt }
                    }
                }
            }
        }.run {
            export(HtmlExporter(ignoreForegroundColor = false, ignoreBackgroundColor = false)).let {
                File("/Users/rune/div/oppgave-mapping-kombinasjoner.html").writeText(it)
            }
            export(ConfluenceWikiExporter()).let {
                File("/Users/rune/div/oppgave-mapping-kombinasjoner.txt").writeText(it)
            }
        }
    }
}
