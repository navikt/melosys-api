package no.nav.melosys.service.oppgave.migrering

import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.LoggingTestUtils
import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.oppgave.OppgaveGosysMapping
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate

@Disabled("brukes bare for å lage oppgave migrering html rapporter, fjerns fra git etter at migreing er utført")
class LagRapportTest {
    private val oppgaveGosysMapping = OppgaveGosysMapping(
        FakeUnleash().apply { enable(OppgaveGosysMapping.NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING) }
    )
    private val oppgaveFactory = OppgaveFactory(FakeUnleash().apply { enable(ToggleName.NY_GOSYS_MAPPING) })

    @Test
    fun `lag rapport hvor kombinasjoner er gruppert etter antall pr kombinasjon og med de som er forskjellig`() {
        val oppgaveGosysMapping = OppgaveGosysMapping(
            FakeUnleash().apply { enable(OppgaveGosysMapping.NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING) }
        )
        val migreringsRapport =
            Migrering.migreringsRapportFraJson("/Users/rune/div/dryrun-0520/jsonrapport-prod-0520.json")

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

                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                    item(bc = "DarkSalmon") { "Beskrivelse" }
                    item(bc = "DarkSalmon") { "Behandlingstema" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                }

                var sumTemaDiffer: Int = 0
                var sumOppgveDiffer: Int = 0
                var sum: Int = 0
                migreringsRapport.sortedMigreringsListe().asSequence()
                    .filter { !it.ny.fantIkkeOppgaveMapping() && it.oppgaver.size == 1 }
                    .filter { it.temaErForskjellig() || it.oppgavetypeErForskjellig() }
                    .map { it.tilSakInfo() }.filter { it.temaDiffer() || it.oppgavetypeDiffer() }.groupBy { it }
                    .map { it.key to it.value.size }
                    .sortedBy { it.second }.toList().reversed().forEach { (sak, count) ->
                        sum += count
                        if (sak.temaDiffer()) sumTemaDiffer += count
                        if (sak.oppgavetypeDiffer()) sumOppgveDiffer += count
                        val ny: OppgaveGosysMapping.Oppgave =
                            oppgaveGosysMapping.finnOppgave(
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

                            item(bc = "PeachPuff") { ny.beskrivelsefelt }
                            item(bc = "PeachPuff") { sak.beskrivelse ?: "" }
                            item(bc = "PeachPuff") { ny.oppgaveBehandlingstema?.kode }
                            item(bc = "PeachPuff") { ny.oppgaveBehandlingstema?.name }

                        }
                    }
                tr {
                    (0..4).forEach { item { "" } }
                    item(bc = "PaleGoldenRod", font = Font.STRONG) { sum }
                    item(bc = "DarkSalmon", font = Font.STRONG) { sumTemaDiffer }
                    item { "" }
                    item(bc = "DarkSalmon", font = Font.STRONG) { sumOppgveDiffer }
                    (0..4).forEach { item { "" } }
                }

            }
        }.run {
            export(HtmlExporter()).let {
                File("/Users/rune/div/forskjell-migrering.html").writeText(it)
            }
            export(ConfluenceWikiExporter()).let {
                File("/Users/rune/div/forskjell-migrering.txt").writeText(it)
            }
        }
    }


    @Test
    fun `lag rapport hvor kombinasjoner er gruppert etter antall pr kombinasjon`() {
        val migreringsRapport =
            Migrering.migreringsRapportFraJson("/Users/rune/div/dryrun-0520/jsonrapport-prod-0520.json")

        document {
            table {
                th {
                    item(bc = "BurlyWood") { "Sakstype" }
                    item(bc = "BurlyWood") { "Sakstema" }
                    item(bc = "BurlyWood") { "Behandlingstype" }
                    item(bc = "BurlyWood") { "Behandlingstema" }
                    item(bc = "Orange") { "Mapping regel" }
                    item(bc = "Orange") { "Antall" }
                    item(bc = "DarkSalmon") { "Beskrivelse" }
                    item(bc = "DarkSalmon") { "Behandlingstema" }
                    item(bc = "DarkSalmon") { "Behandlingstema beskrivelse" }
                    item(bc = "DarkSalmon") { "Tema" }
                    item(bc = "DarkSalmon") { "Oppgavetype" }
                    item(bc = "DarkSalmon") { "Beskrivelsesfelt" }
                }

                var sum: Int = 0
                migreringsRapport.sortedMigreringsListe().asSequence()
                    .filter { !it.ny.fantIkkeOppgaveMapping() && it.oppgaver.size == 1 }
                    .filter { it.mangerSedDokument() }
                    .map { it.tilSak() }.groupBy { it }
                    .map { it.key to it.value.size }
                    .sortedBy { it.second }.toList().reversed().forEach {
                        sum += it.second
                        val sak = it.first
                        val oppgave: OppgaveGosysMapping.Oppgave =
                            oppgaveGosysMapping.finnOppgave(
                                sak.sakstype,
                                sak.sakstema,
                                sak.behandlingstema,
                                sak.behandlingstype
                            )
                        val fargeFraRegelTuffet = when (oppgave.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                            OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> "LightSalmon"
                        }
                        val fontType = when (oppgave.regelTruffet) {
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
                            item(bc = fargeFraRegelTuffet, font = fontType) { oppgave.regelTruffet.beskrivelse }
                            item(bc = "PaleGoldenRod", font = Font.STRONG) { it.second }
                            item(bc = "PaleGoldenRod") { sak.beskrivelse }
                            item(bc = "PeachPuff") { oppgave.oppgaveBehandlingstema?.kode }
                            item(bc = "PeachPuff") { oppgave.oppgaveBehandlingstema?.name }
                            item(bc = "PeachPuff") { oppgave.tema }
                            item(bc = "PeachPuff") { oppgave.oppgaveType }
                            item(bc = "PeachPuff") { oppgave.beskrivelsefelt }

                        }
                    }
                tr {
                    (0..4).forEach { item { "" } }
                    item(bc = "PaleGoldenRod", font = Font.STRONG) { sum }
                    (0..5).forEach { item { "" } }
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
                    item(bc = "DarkSalmon") { "Beskrivelse" }
                    item(bc = "DarkSalmon") { "Warning" }
                }
                GyldigeKombinasjoner.rowsMelosysOgDatavarehus.sortedWith(
                    compareBy(
                        { it.sakstype },
                        { it.sakstema },
                        { it.behandlingstype },
                        { it.behandlingstema }
                    )
                ).forEachIndexed { index, it ->
                    LoggingTestUtils.withLogAppender<OppgaveFactory> { oppgaveFactoryLog ->
                        val oppgave: OppgaveGosysMapping.Oppgave =
                            oppgaveGosysMapping.finnOppgave(
                                it.sakstype,
                                it.sakstema,
                                it.behandlingstema,
                                it.behandlingstype
                            )
                        val sedType = when (oppgave.beskrivelsefelt) {
                            OppgaveGosysMapping.Beskrivelsefelt.TOMT, OppgaveGosysMapping.Beskrivelsefelt.BEHANDLINGSTEMA, OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR -> null
                            OppgaveGosysMapping.Beskrivelsefelt.SED -> if (oppgave.oppgaveBehandlingstema?.kode == "ab0482") null else SedType.A003
                            OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT -> if (index % 2 == 1) SedType.A004 else null
                        }
                        val behandling = lagBehandling(
                            it.sakstype,
                            it.sakstema,
                            it.behandlingstema,
                            it.behandlingstype,
                            sedType
                        )
                        val resultatOppgave = oppgaveFactory.lagBehandlingsoppgave(
                            behandling, LocalDate.now()
                        ) { behandling.finnSedDokument().orElse(null) }.build()

                        val fargeFraRegelTuffet = when (oppgave.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> "MintCream"
                            OppgaveGosysMapping.Regel.HENVENDELSE -> "MistyRose"
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> "Orchid"
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> "LightCoral"
                        }
                        val fontType = when (oppgave.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> Font.NORMAL
                            OppgaveGosysMapping.Regel.HENVENDELSE -> Font.ITALIC
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> Font.STRONG
                            OppgaveGosysMapping.Regel.KUN_VED_MIGRERING -> Font.STRONG
                        }
                        tr {
                            item { it.sakstype }
                            item { it.sakstema }
                            item { it.behandlingstype }
                            item { it.behandlingstema }
                            item(bc = fargeFraRegelTuffet, font = fontType) { oppgave.regelTruffet.beskrivelse }
                            item { it.regel.beskrivelse }
                            item { oppgave.oppgaveBehandlingstema?.kode }
                            item { oppgave.oppgaveBehandlingstema?.name }
                            item { oppgave.tema }
                            item { oppgave.oppgaveType }
                            item { oppgave.beskrivelsefelt }
                            item { resultatOppgave.beskrivelse }
                            item { oppgaveFactoryLog.list.joinToString(",") }
                        }
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

    private fun lagBehandling(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        sedType: SedType? = null
    ): Behandling {
        return Behandling().apply {
            id = 1
            fagsak = Fagsak().apply {
                saksnummer = "MEL-1"
                type = sakstype
                tema = sakstema
            }
            type = behandlingstype
            tema = behandlingstema
            if (sedType != null) {
                saksopplysninger.add(Saksopplysning().apply {
                    type = SaksopplysningType.SEDOPPL
                    dokument = SedDokument().apply {
                        setSedType(sedType)
                    }
                })
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
        val orgOppgavetype: String? = null,
        val orgTema: String? = null,
    ) {
        fun temaDiffer(): Boolean = nyTema != orgTema

        fun oppgavetypeDiffer(): Boolean = nyOppgavetype != orgOppgavetype
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
            oppgaver.first().oppgavetype.kode,
            oppgaver.first().tema.kode
        )
    }

    fun MigreringsSak.mangerSedDokument(): Boolean =
        try {
            val oppgave =
                oppgaveGosysMapping.finnOppgave(sak.sakstype, sak.sakstema, sak.behandlingstema, sak.behandlingstype)
            oppgave.beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.SED && ny.beskrivelse.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
}
