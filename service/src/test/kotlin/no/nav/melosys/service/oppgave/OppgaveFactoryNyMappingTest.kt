package no.nav.melosys.service.oppgave

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Tema
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveFactoryNyMappingTest {

    private val oppgaveFactory = OppgaveFactory(FakeUnleash().apply { enable(ToggleName.NY_GOSYS_MAPPING) })
    private val oppgaveGosysMapping = OppgaveGosysMapping()

    @Test
    fun `skal kun ha ett treff på alle mulige kombinasjoner av sakstype, sakstema, behandlingstype og behandlingstema`() {

        oppgaveGosysMapping.rows.forEach { row ->
            row.behandlingstema.forEach { behandlingstema ->
                row.behandlingstype.forEach { behandlingstyper ->
                    oppgaveGosysMapping.rows.filter {
                        it.sakstype == row.sakstype &&
                            it.sakstema == row.sakstema &&
                            behandlingstyper in it.behandlingstype &&
                            behandlingstema in it.behandlingstema
                    }.shouldHaveSize(1)
                }
            }
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

    @ParameterizedTest(name = "{0} - {1} - {2} - {3}")
    @MethodSource("gyldigHenvendleseKombinasjonerBortsettFraRegistretTabell")
    fun `gyldig henvendlese kombinasjoner bortsett fra data registert i tabell`(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        expected: String
    ) {
        val oppgave =
            oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, Behandlingstyper.HENVENDELSE)


        oppgave.apply {
            oppgaveBehandlingstema.kode.shouldBe(expected)
            oppgaveType.shouldBe(Oppgavetyper.VURD_HENV)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT)
        }
    }

    private fun gyldigHenvendleseKombinasjonerBortsettFraRegistretTabell() =
        sequence<Arguments> {
            val oppgaveGosysMapping = OppgaveGosysMapping()
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
