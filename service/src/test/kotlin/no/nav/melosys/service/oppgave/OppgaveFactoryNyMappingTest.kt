package no.nav.melosys.service.oppgave

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveFactoryNyMappingTest {

    private val oppgaveFactory = OppgaveFactory(FakeUnleash().apply { enable(ToggleName.NY_GOSYS_MAPPING) })
    private val oppgaveGosysMapping = OppgaveGosysMapping()

    @Test
    fun `Sed skal brukes som beskrivelse ved oppgavetype BEH_SED - untatt ved A1_ANMODNING_OM_UNNTAK_PAPIR`() {
        oppgaveGosysMapping.rows
            .filter {
                it.oppgave.oppgaveType == Oppgavetyper.BEH_SED
            }.filter {
                it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
            }.forEach { row ->
                val behandling = lagBehandlingBrukFørsteRad(row, SedType.A003)

                val oppgave = oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()).build()

                oppgave.beskrivelse.shouldBe(SedType.A003.name)
            }
    }

    @Test
    fun `tom beskrivelse når oppgavetype ikke er BEH_SED - untatt ved A1_ANMODNING_OM_UNNTAK_PAPIR`() {
        oppgaveGosysMapping.rows
            .filter {
                it.oppgave.oppgaveType != Oppgavetyper.BEH_SED
            }.filter {
                it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
            }.forEach { row ->
                val behandling = lagBehandlingBrukFørsteRad(row)

                val oppgave = oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()).build()

                oppgave.beskrivelse.shouldBe("")
            }
    }

    @Test
    fun `A1_ANMODNING_OM_UNNTAK_PAPIR skal brukes som beskrivelse ved A1_ANMODNING_OM_UNNTAK_PAPIR`() {
        oppgaveGosysMapping.rows
            .filter {
                it.oppgave.beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
            }.shouldHaveSize(1).forEach { row ->
                val behandling = lagBehandlingBrukFørsteRad(row)

                val oppgave = oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()).build()

                oppgave.beskrivelse.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR.beskrivelse)
            }
    }

    @Test
    fun `oppgave tema skal være av riktig type`() {
        oppgaveGosysMapping.rows.forEach { row ->
            row.behandlingstema.forEach { behandlingstema ->
                val tema: Tema = oppgaveFactory.utledTema(row.sakstype, row.sakstema, behandlingstema)
                tema.shouldBe(row.oppgave.tema)
            }
        }
    }

    @ParameterizedTest(name = "{0}, {1}, {2}, {3} -> {4}")
    @MethodSource("fraRegistretTabell")
    fun `oppgave oppslag skal fungere på med type sed`(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstyper: Behandlingstyper,
        expectedKode: OppgaveGosysMapping.Oppgave
    ) {
        val oppgave = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstyper)

        oppgave.oppgaveBehandlingstema.shouldBe(expectedKode.oppgaveBehandlingstema)
    }

    @ParameterizedTest(name = "{0}, {1}, {2}, {3} -> {4}")
    @MethodSource("fraRegistretTabell")
    fun `oppgave oppslag skal fungere på alle kombinasjonene i tabell`(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstyper: Behandlingstyper,
        expectedKode: OppgaveGosysMapping.Oppgave
    ) {
        val oppgave = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstyper)

        oppgave.oppgaveBehandlingstema.shouldBe(expectedKode.oppgaveBehandlingstema)
    }

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("gyldigHenvendleseKombinasjonerBortsettIkkeRegistretITabell")
    fun `gyldig henvendlese kombinasjoner bortsett fra data registert i tabell`(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        expectedOppgaveBehandlingstema: String
    ) {
        val oppgave = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, Behandlingstyper.HENVENDELSE)

        oppgave.apply {
            oppgaveBehandlingstema.kode.shouldBe(expectedOppgaveBehandlingstema)
            oppgaveType.shouldBe(Oppgavetyper.VURD_HENV)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT)
        }
    }

    private fun lagBehandlingBrukFørsteRad(
        tableRow: OppgaveGosysMapping.TableRow,
        sedType: SedType? = null
    ): Behandling =
        lagBehandling(
            tableRow.sakstype,
            tableRow.sakstema,
            tableRow.behandlingstema.first(),
            tableRow.behandlingstype.first(),
            sedType
        )

    private fun lagBehandling(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        sedType: SedType? = null
    ): Behandling {
        return Behandling().apply {
            fagsak = Fagsak().apply {
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

    private fun fraRegistretTabell() =
        sequence<Arguments> {
            oppgaveGosysMapping.rows.forEach { row ->
                row.behandlingstema.forEach { behandlingstema ->
                    row.behandlingstype.forEach { behandlingstyper ->
                        yield(
                            arguments(
                                row.sakstype,
                                row.sakstema,
                                behandlingstema,
                                behandlingstyper,
                                row.oppgave
                            )
                        )
                    }
                }
            }
        }.toList()

    private fun gyldigHenvendleseKombinasjonerBortsettIkkeRegistretITabell() =
        sequence<Arguments> {
            Sakstyper.values().forEach { sakstyper: Sakstyper ->
                Sakstemaer.values().forEach { sakstemaer: Sakstemaer ->
                    Behandlingstema.values().filter { behandlingstema ->
                        oppgaveGosysMapping.finnOppgaveFraTabell(
                            sakstyper,
                            sakstemaer,
                            behandlingstema,
                            Behandlingstyper.HENVENDELSE
                        ) == null
                    }.forEach { behandlingstema ->
                        val oppgave = oppgaveGosysMapping.finnOppgaveVedBehandlingstypeHenvendelse(
                            sakstyper,
                            behandlingstema,
                        )
                        if (oppgave != null) {
                            yield(
                                arguments(
                                    sakstyper,
                                    sakstemaer,
                                    behandlingstema,
                                    oppgave.oppgaveBehandlingstema.kode
                                )
                            )
                        }
                    }
                }
            }
        }.toList()
}
