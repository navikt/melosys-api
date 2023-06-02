package no.nav.melosys.service.oppgave

import ch.qos.logback.classic.Level
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.spyk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.LoggingTestUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppgaveFactoryNyMappingTest {

    private val oppgaveFactory = OppgaveFactory()

    @Test
    fun `Sed skal brukes som beskrivelse ved oppgavetype BEH_SED - untatt ved A1_ANMODNING_OM_UNNTAK_PAPIR eller oppgave opprettes fra eksisterende`() {
        rowsMedAlleKombinasjoner
            .filter {
                it.oppgave.oppgaveType == Oppgavetyper.BEH_SED
            }.filter {
                it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
            }.shouldHaveSize(19)
            .filter { sak ->
                sak.behandlingstype !in listOf(
                    Behandlingstyper.NY_VURDERING,
                    Behandlingstyper.KLAGE,
                    Behandlingstyper.ENDRET_PERIODE
                )
            }.shouldHaveSize(8).forEach { sak ->
                val behandling = sak.lagBehandlingMedSpy(SedType.A003)
                val oppgave =
                    oppgaveFactory.lagBehandlingsoppgave(
                        behandling, LocalDate.now(), behandling::hentSedDokument
                    ).build()

                withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                    oppgave.beskrivelse.shouldBe(SedType.A003.name)
                    verify { behandling.saksopplysninger }
                }
            }
    }

    @Test
    fun `tom beskrivelse når oppgavetype ikke er BEH_SED - untatt ved A1_ANMODNING_OM_UNNTAK_PAPIR og BEHANDLINGSTEMA`() {
        rowsMedAlleKombinasjoner
            .filter {
                it.oppgave.oppgaveType != Oppgavetyper.BEH_SED
            }.filter {
                it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
            }.filter {
                it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.BEHANDLINGSTEMA
            }.shouldHaveSize(55).forEach { sak ->
                val behandling = sak.lagBehandlingMedSpy()
                val oppgave =
                    oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now(), behandling::hentSedDokument)
                        .build()

                withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                    oppgave.beskrivelse.shouldBe("")
                    verify { behandling.hentSedDokument() wasNot called }
                }
            }
    }

    @Test
    fun `A1_ANMODNING_OM_UNNTAK_PAPIR skal brukes som beskrivelse ved A1_ANMODNING_OM_UNNTAK_PAPIR`() {
        rowsMedAlleKombinasjoner
            .filter {
                it.oppgave.beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
            }.shouldHaveSize(3).forEach { sak ->
                val behandling = sak.lagBehandlingMedSpy()
                val oppgave =
                    oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now(), behandling::hentSedDokument)
                        .build()

                withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                    oppgave.beskrivelse.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR.beskrivelse)
                    verify { behandling.hentSedDokument() wasNot called }
                }
            }
    }

    @Test
    fun `Bruker sed om det finnes for de 14 som har SED_ELLER_TOMT, ellers ikke log warning og retuner en tom streng`() {
        rowsMedAlleKombinasjoner
            .filter {
                it.oppgave.beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT
            }.shouldHaveSize(14).forEach { sak ->
                val behandling = sak.lagBehandlingMedSpy()
                LoggingTestUtils.withLogAppender<OppgaveFactory> { oppgaveFactoryListAppender ->
                    val oppgave =
                        oppgaveFactory.lagBehandlingsoppgave(
                            behandling,
                            LocalDate.now()
                        ) { finnSedDokument(behandling) }
                            .build()

                    withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                        oppgave.beskrivelse.shouldBe("")
                        oppgaveFactoryListAppender.list.shouldHaveSize(0)
                        verify { behandling.finnSedDokument() }
                    }
                }
            }
    }

    @Test
    fun `Oppgavetyper BEH_SED skal føre til Beskrivelsefelt SED eller SED_ELLER_TOMT`() {
        rowsMedAlleKombinasjoner
            .filter {
                it.oppgave.oppgaveType == Oppgavetyper.BEH_SED
            }.forEach { sak ->
                withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                    sak.oppgave.beskrivelsefelt in listOf(
                        OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT,
                        OppgaveGosysMapping.Beskrivelsefelt.SED
                    )
                }
            }
    }

    @Test
    fun `Bruker sed om det finnes for de 5 som har SED, ellers log warning og retuner en tom streng`() {
        rowsMedAlleKombinasjoner
            .filter {
                it.oppgave.beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.SED
            }.shouldHaveSize(5).forEach { sak ->
                val behandling = sak.lagBehandlingMedSpy()
                LoggingTestUtils.withLogAppender<OppgaveFactory> { oppgaveFactoryListAppender ->
                    val oppgave =
                        oppgaveFactory.lagBehandlingsoppgave(
                            behandling,
                            LocalDate.now(),
                        ) { finnSedDokument(behandling) }
                            .build()

                    withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                        oppgave.beskrivelse.shouldBe("")
                        oppgaveFactoryListAppender.list
                            .shouldHaveSize(1)
                            .first().run {
                                level.shouldBe(Level.WARN)
                                message.shouldBe("Sed dokument mangler for:MEL-1 behandlingID:1")
                            }
                    }
                }
            }
    }

    @Test
    fun `skal ikke logge advarsel når oppgave opprettes fra eksisterende og SED er tilgjengelig`() {
        rowsMedAlleKombinasjoner.filter {
            it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR
        }.filter {
            it.oppgave.beskrivelsefelt != OppgaveGosysMapping.Beskrivelsefelt.BEHANDLINGSTEMA
        }.filter {
            it.behandlingstype in listOf(Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)
        }.shouldHaveSize(44).forEach { sak ->
            LoggingTestUtils.withLogAppender<OppgaveBeskrivelseNyUtleder> { oppgaveBeskrivelseListAppender ->
                val behandling = sak.lagBehandlingMedSpy()
                val oppgave =
                    oppgaveFactory.lagBehandlingsoppgave(
                        behandling,
                        LocalDate.now(),
                    ) { finnSedDokument(behandling) }
                        .build()

                withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}, ${sak.behandlingstype}") {
                    oppgave.beskrivelse.shouldBe("")
                    oppgaveBeskrivelseListAppender.list
                        .shouldHaveSize(0)
                }
            }
        }
    }


    @Test
    fun `oppgave tema skal være av riktig type`() {
        rowsMedAlleKombinasjoner.forEach { sak ->
            val tema: Tema = oppgaveFactory.utledTema(sak.sakstype, sak.sakstema, sak.behandlingstema)

            withClue("sakstype${sak.sakstype}, sakstema=${sak.sakstema}, behandlingstema:${sak.behandlingstema}") {
                tema.shouldBe(sak.oppgave.tema)
            }
        }
    }

    @Test
    fun `AVTALAND_FORESPORSEL_FRA_TRYGDEMYNDIGHET skal ikke brukes`() {
        val oppgave = oppgaveGosysMapping.finnOppgave(
            Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, Behandlingstyper.HENVENDELSE
        )

        oppgave.oppgaveBehandlingstema?.kode.shouldBe(null)
    }

    @Test
    fun `Tema må hentes ved bruk av teamaUtleder`() {
        val oppgave = oppgaveGosysMapping.finnOppgave(
            Sakstyper.EU_EOS, Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, Behandlingstyper.HENVENDELSE
        )

        oppgave.tema.shouldBe(Tema.UFM)
    }

    @Test
    fun `Beskrivelsesfelt fylles ut basert på behandlingstema i Melosys for EU_EØS, MEDLEMSKAP_LOVVALG som mapper til ab0483`() {
        val behandling = lagBehandling(
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstyper.FØRSTEGANG
        )
        val oppgave =
            oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now(), behandling::hentSedDokument).build()

        oppgave.beskrivelse.shouldBe(Behandlingstema.UTSENDT_ARBEIDSTAKER.beskrivelse)
    }


    @Test
    fun `Skal ha SED - om ikke skal det logges en warning - og retunere en tom streng`() {
        LoggingTestUtils.withLogAppender<OppgaveFactory> { oppgaveFactoryListAppender ->
            val behandling = lagBehandling(
                Sakstyper.EU_EOS,
                Sakstemaer.UNNTAK,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                Behandlingstyper.FØRSTEGANG
            )
            val oppgave =
                oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()) { finnSedDokument(behandling) }
                    .build()

            oppgaveFactoryListAppender.list
                .shouldHaveSize(1)
                .first().run {
                    level.shouldBe(Level.WARN)
                    message.shouldBe("Sed dokument mangler for:MEL-1 behandlingID:1")
                }
            oppgave.beskrivelse.shouldBe("")
        }
    }

    fun finnSedDokument(behandling: Behandling): SedDokument? = behandling.finnSedDokument().orElse(null)

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
        expectedOppgaveBehandlingstema: String?
    ) {
        val oppgave = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, Behandlingstyper.HENVENDELSE)

        oppgave.apply {
            oppgaveBehandlingstema?.kode.shouldBe(expectedOppgaveBehandlingstema)
            oppgaveType.shouldBe(Oppgavetyper.VURD_HENV)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT)
        }
    }

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("henvendelseVirksomhetPermutasjoner")
    fun `hånter henvendelse og virksomhet med egne regler`(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) {
        val oppgave = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype)

        oppgave.apply {
            tema.shouldBe(OppgaveTemaUtleder().utledTema(sakstype, sakstema, behandlingstema))
            oppgaveBehandlingstema?.kode.shouldBe(null)
            oppgaveType.shouldBe(Oppgavetyper.VURD_HENV)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.TOMT)
        }
    }

    private data class TableRowSak(
        val sakstype: Sakstyper,
        val sakstema: Sakstemaer,
        val behandlingstype: Behandlingstyper,
        val behandlingstema: Behandlingstema,
        val oppgave: OppgaveGosysMapping.Oppgave
    ) {
        fun lagBehandlingMedSpy(sedType: SedType? = null): Behandling {
            return spyk(
                lagBehandling(
                    sakstype,
                    sakstema,
                    behandlingstema,
                    behandlingstype,
                    sedType
                )
            )
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
            Sakstyper.values().forEach { sakstype: Sakstyper ->
                Sakstemaer.values().forEach { sakstemae: Sakstemaer ->
                    Behandlingstema.values().filter { behandlingstema ->
                        oppgaveGosysMapping.finnOppgaveFraTabell(
                            sakstype,
                            sakstemae,
                            behandlingstema,
                            Behandlingstyper.HENVENDELSE
                        ) == null
                    }.forEach { behandlingstema ->
                        val oppgave = oppgaveGosysMapping.finnOppgaveVedBehandlingstypeHenvendelse(
                            sakstype,
                            sakstemae,
                            behandlingstema,
                            Behandlingstyper.HENVENDELSE
                        )
                        if (oppgave != null) {
                            yield(
                                arguments(
                                    sakstype,
                                    sakstemae,
                                    behandlingstema,
                                    oppgave.oppgaveBehandlingstema?.kode
                                )
                            )
                        }
                    }
                }
            }
        }.toList()

    private fun henvendelseVirksomhetPermutasjoner() =
        sequence<Arguments> {
            Sakstyper.values().forEach { sakstype: Sakstyper ->
                Sakstemaer.values().forEach { sakstemae: Sakstemaer ->
                    yield(
                        arguments(
                            sakstype,
                            sakstemae,
                            Behandlingstema.VIRKSOMHET,
                            Behandlingstyper.HENVENDELSE,
                        )
                    )
                }
            }
        }.toList()

    companion object {
        private val oppgaveGosysMapping = OppgaveGosysMapping()

        private val rowsMedAlleKombinasjoner by lazy {
            sequence {
                oppgaveGosysMapping.rows.forEach { tableRow ->
                    tableRow.behandlingstema.forEach { behandlingstema ->
                        tableRow.behandlingstype.forEach { behandlingstype ->
                            yield(
                                TableRowSak(
                                    tableRow.sakstype,
                                    tableRow.sakstema,
                                    behandlingstype,
                                    behandlingstema,
                                    tableRow.oppgave
                                )
                            )
                        }
                    }
                }
            }.toList()
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
}
