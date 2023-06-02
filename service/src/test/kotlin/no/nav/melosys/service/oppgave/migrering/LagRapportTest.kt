package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.oppgave.OppgaveGosysMapping
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled("brukes for å lage rapport om manglende oppgaver")
class LagRapportTest {

    @Test
    fun `mangler oppgave`() {
        val migreringsRapport =
            Migrering.migreringsRapportFraJson("/Users/rune/div/prod-0601/jsonrapport-prod-0601.json")

        val filterVekk =
            Migrering.migreringsRapportFraJson("/Users/rune/div/dryrun-0520/jsonrapport-prod-0520.json")
                .sortedMigreringsListe().filter { it.oppgaver.isEmpty() }.map { it.sak.saksnummer }.toSet()

        File("/Users/rune/div/oppgave-mangler-diff-2.html").writeText(migreringsRapport.html { migreringsSaker ->
            migreringsSaker
                .filterNot { filterVekk.contains(it.sak.saksnummer) }
                .filter { it.oppgaver.isEmpty() }

        })

        document {
            table {
                th {
                    item(bc = "BurlyWood") { "Sakstype" }
                    item(bc = "BurlyWood") { "Sakstema" }
                    item(bc = "BurlyWood") { "Behandlingstype" }
                    item(bc = "BurlyWood") { "Behandlingstema" }
                    item(bc = "Orange") { "Mapping regel" }
                    item(bc = "Orange") { "Antall" }

                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                    item(bc = "DarkSalmon") { "Beskrivelse" }
                    item(bc = "DarkSalmon") { "Behandlingstema" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                }

                var sum = 0
                migreringsRapport.sortedMigreringsListe().asSequence()
//                    .filterNot { filterVekk.contains(it.sak.saksnummer) }
                    .filter { it.oppgaver.isEmpty() }
                    .filter {
                        OppgaveMigrering.erGyldig(
                            it.sak.sakstype,
                            it.sak.sakstema,
                            it.sak.behandlingstype,
                            it.sak.behandlingstema
                        )
                    }
                    .onEach {
//                        println(it.sak.saksnummer)
                    }
                    .map { it.tilSak() }
                    .groupBy { it }
                    .map { it.key to it.value.size }
                    .sortedBy { it.second }.toList().reversed().forEach { (sak, count) ->
                        sum += count
                        val ny: OppgaveGosysMapping.Oppgave =
                            OppgaveMigrering.finnOppgave(
                                sak.sakstype,
                                sak.sakstema,
                                sak.behandlingstema,
                                sak.behandlingstype
                            )
                        val fargeFraRegelTuffet = when (ny.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                            OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> "LightSalmon"
                        }
                        val fontFraRegelTruffet = when (ny.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> Font.NORMAL
                            OppgaveGosysMapping.Regel.HENVENDELSE -> Font.ITALIC
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> Font.STRONG
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> Font.ITALIC
                        }
                        tr {
                            item(bc = "OldLace") { sak.sakstype }
                            item(bc = "OldLace") { sak.sakstema }
                            item(bc = "OldLace") { sak.behandlingstype }
                            item(bc = "OldLace") { sak.behandlingstema }
                            item(bc = fargeFraRegelTuffet, font = fontFraRegelTruffet) { ny.regelTruffet.beskrivelse }
                            item(bc = "PaleGoldenRod", font = Font.STRONG) { count }

                            item(bc = "PeachPuff") { ny.beskrivelsefelt }
                            item(bc = "PeachPuff") { sak.beskrivelse ?: "" }
                            item(bc = "PeachPuff") { ny.oppgaveBehandlingstema?.kode }
                            item(bc = "PeachPuff") { ny.oppgaveBehandlingstema?.name }

                        }
                    }
                tr {
                    (0..4).forEach { item { "" } }
                    item(bc = "PaleGoldenRod", font = Font.STRONG) { sum }
                    (0..4).forEach { item { "" } }
                }

            }
        }.run {
            export(HtmlExporter()).let {
                File("/Users/rune/div/mangler-oppgave.html").writeText(it)
            }
        }
    }
    @Test
    fun `rapport over oppgaver som er blitt forandtert slik at de er forsjellig fra migreringen`() {
        val migreringsRapport =
            Migrering.migreringsRapportFraJson("/Users/rune/div/dryrun-prod-0602/jsonrapport-prod-dryrun-0602.json")

        document {
            table {
                th {
                    item(bc = "BurlyWood") { "Sakstype" }
                    item(bc = "BurlyWood") { "Sakstema" }
                    item(bc = "BurlyWood") { "Behandlingstype" }
                    item(bc = "BurlyWood") { "Behandlingstema" }
                    item(bc = "Orange") { "Mapping regel" }
                    item(bc = "Orange") { "Antall" }

                    item(bc = "DarkSalmon") { "NyTema" }
                    item(bc = "DarkSalmon") { "OrgTema" }
                    item(bc = "DarkSalmon") { "NyOppgavetype" }
                    item(bc = "DarkSalmon") { "OrgOppgavetype" }
                    item(bc = "DarkSalmon") { "NyBehandlingstema" }
                    item(bc = "DarkSalmon") { "OrgBehandlingstema" }

                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                    item(bc = "DarkSalmon") { "Beskrivelse" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                }

                var sumTemaDiffer = 0
                var sumOppgveDiffer = 0
                var sum = 0
                migreringsRapport.sortedMigreringsListe().asSequence()
                    .filter { OppgaveMigrering.erGyldig(it.sak.sakstype, it.sak.sakstema, it.sak.behandlingstype, it.sak.behandlingstema) }
                    .filter { it.oppgaver.size == 1 }
                    .filterNot { it.erOppgaveMigrert() }
                    .map { it.tilSakInfo() }
                    .groupBy { it }
                    .map { it.key to it.value.size }
                    .sortedBy { it.second }.toList().reversed().forEach { (sak, count) ->
                        sum += count
                        if (sak.temaDiffer()) sumTemaDiffer += count
                        if (sak.oppgavetypeDiffer()) sumOppgveDiffer += count
                        val ny: OppgaveGosysMapping.Oppgave =
                            OppgaveMigrering.finnOppgave(
                                sak.sakstype,
                                sak.sakstema,
                                sak.behandlingstema,
                                sak.behandlingstype
                            )
                        val fargeFraRegelTuffet = when (ny.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                            OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> "LightSalmon"
                        }
                        val fontFraRegelTruffet = when (ny.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> Font.NORMAL
                            OppgaveGosysMapping.Regel.HENVENDELSE -> Font.ITALIC
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> Font.STRONG
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> Font.ITALIC
                        }
                        val fontFraTemaForskjellig = when (sak.temaDiffer()) {
                            true -> Font.STRONG
                            false -> Font.NORMAL
                        }
                        val fontFraOppgaveTypeForskjellig = when (sak.oppgavetypeDiffer()) {
                            true -> Font.STRONG
                            false -> Font.NORMAL
                        }
                        val fontFraOppgaveBehandlingstemaForskjellig = when (sak.oppgaveBehandlingstemaDiffer()) {
                            true -> Font.STRONG
                            false -> Font.NORMAL
                        }
                        tr {
                            item(bc = "OldLace") { sak.sakstype }
                            item(bc = "OldLace") { sak.sakstema }
                            item(bc = "OldLace") { sak.behandlingstype }
                            item(bc = "OldLace") { sak.behandlingstema }
                            item(bc = fargeFraRegelTuffet, font = fontFraRegelTruffet) { ny.regelTruffet.beskrivelse }
                            item(bc = "PaleGoldenRod", font = Font.STRONG) { count }

                            item(bc = "PeachPuff", font = fontFraTemaForskjellig) { ny.tema.kode }
                            item(bc = "PeachPuff", font = fontFraTemaForskjellig) { sak.orgTema }
                            item(bc = "PeachPuff", font = fontFraOppgaveTypeForskjellig) { ny.oppgaveType.kode }
                            item(bc = "PeachPuff", font = fontFraOppgaveTypeForskjellig) { sak.orgOppgavetype }
                            item(bc = "PeachPuff", font = fontFraOppgaveBehandlingstemaForskjellig) { ny.oppgaveBehandlingstema?.kode }
                            item(bc = "PeachPuff", font = fontFraOppgaveBehandlingstemaForskjellig) { sak.orgBehandlingstema }

                            item(bc = "PeachPuff") { ny.beskrivelsefelt }
                            item(bc = "PeachPuff") { sak.beskrivelse ?: "" }
                            item(bc = "PeachPuff") { ny.oppgaveBehandlingstema?.name }

                        }
                    }
                tr {
                    (0..4).forEach { item { "" } }
                    item(bc = "PaleGoldenRod", font = Font.STRONG) { sum }
                    item(bc = "DarkSalmon", font = Font.STRONG) { sumTemaDiffer }
                    item { "" }
                    item(bc = "DarkSalmon", font = Font.STRONG) { sumOppgveDiffer }
                    (0..6).forEach { item { "" } }
                }

            }
        }.run {
            export(HtmlExporter()).let {
                File("/Users/rune/div/forskjell-migrering.html").writeText(it)
            }
        }
    }

    private data class tilSakInfo(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstype: Behandlingstyper,
        val behandlingstema: Behandlingstema,
        val beskrivelse: String? = null,
        val nyOppgavetype: String? = null,
        val nyTema: String? = null,
        val nyBehandlingstema: String? = null,
        val orgOppgavetype: String? = null,
        val orgTema: String? = null,
        val orgBehandlingstema: String? = null,
    ) {
        fun temaDiffer(): Boolean = nyTema != orgTema

        fun oppgavetypeDiffer(): Boolean = nyOppgavetype != orgOppgavetype

        fun oppgaveBehandlingstemaDiffer(): Boolean = nyBehandlingstema != orgBehandlingstema
    }

    private fun MigreringsSak.tilSakInfo(): tilSakInfo {
        return tilSakInfo(
            sak.sakstype,
            sak.sakstema,
            sak.behandlingstype,
            sak.behandlingstema,
            ny.beskrivelse,
            ny.oppgaveType?.kode,
            ny.tema?.kode,
            ny.oppgaveBehandlingstema?.kode,
            oppgaver.first().oppgavetype.kode,
            oppgaver.first().tema.kode,
            oppgaver.first().behandlingstema
        )
    }
}
