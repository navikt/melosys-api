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
    private val oppgaveGosysMapping = OppgaveGosysMapping(FakeUnleash())
    private val oppgaveFactory = OppgaveFactory(FakeUnleash().apply { enable(ToggleName.NY_GOSYS_MAPPING) })

    @Test
    fun `lag rapport hvor kombinasjoner er gruppert etter antall pr kombinasjon`() {
        val oppgaveGosysMapping = OppgaveGosysMapping(
            FakeUnleash().apply { enable(OppgaveGosysMapping.NY_GOSYS_MAPPING_UNTAKK_FOR_MIGRERING) }
        )
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
                        }
                        val fontType = when (oppgave.regelTruffet) {
                            OppgaveGosysMapping.Regel.FRA_TABELL -> Font.NORMAL
                            OppgaveGosysMapping.Regel.HENVENDELSE -> Font.ITALIC
                            OppgaveGosysMapping.Regel.HENVENDELSE_OG_VIRKSOMHET -> Font.STRONG
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

}
