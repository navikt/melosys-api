package no.nav.melosys.service.oppgave

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFactoryNyMappingTest {

    val oppgaveFactory = OppgaveFactory(FakeUnleash().apply { enable(ToggleName.NY_GOSYS_MAPPING) })

    @Test
    fun `skal kun ha ett treff på alle mulige kombinasjoner av sakstype, sakstema, behandlingstype og behandlingstema`() {
        val oppgaveGoSysMapping = OppgaveGoSysMapping()
        oppgaveGoSysMapping.rows.forEach { row ->
            row.behandlingstema.forEach { behandlingstema ->
                row.behandlingstype.forEach { behandlingstyper ->
                    oppgaveGoSysMapping.rows.filter {
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
        val oppgaveGoSysMapping = OppgaveGoSysMapping()
        oppgaveGoSysMapping.rows.forEach { row ->
            row.behandlingstema.forEach { behandlingstema ->
                val tema: Tema = oppgaveFactory.utledTema(row.sakstype, row.sakstema, behandlingstema)
                tema.shouldBe(row.oppgave.tema)
            }
        }
    }

    @Test
    fun eueos_medlemskapLovvalg_standardEndretPeriode_flere_1() {
        test(
            sakstyper = listOf(Sakstyper.EU_EOS),
            sakstemaer = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
            behandlingstyper = listOf(
                Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.ENDRET_PERIODE,
            ),
            melosysBehandlingstemaer = listOf(
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Behandlingstema.UTSENDT_SELVSTENDIG,
                Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
                Behandlingstema.ARBEID_FLERE_LAND,
                Behandlingstema.ARBEID_KUN_NORGE
            ),
            expectedBehandlingstema = OppgaveBehandlingstema.EU_EOS_YRKESAKTIV,
            expectedBehandlingstype = null,
            expectedTema = Tema.MED,
            expectedOppgavetype = Oppgavetyper.BEH_SAK_MK,
            "TOMT" // bedre test her når vi henter fra SED fra behandling
        )
    }

    private fun test(
        sakstyper: Collection<Sakstyper>,
        sakstemaer: Collection<Sakstemaer>,
        behandlingstyper: Collection<Behandlingstyper>,
        melosysBehandlingstemaer: Collection<Behandlingstema>,
        expectedBehandlingstema: OppgaveBehandlingstema,
        expectedBehandlingstype: OppgaveBehandlingstype?,
        expectedTema: Tema,
        expectedOppgavetype: Oppgavetyper,
        forventetBegrunnelse: String? = null
    ) {
        sakstyper.forEach { sakstype ->
            sakstemaer.forEach { sakstema ->
                behandlingstyper.forEach { behandlignstype ->
                    melosysBehandlingstemaer.forEach { melosysBehandlingstema ->
                        test(
                            sakstype,
                            sakstema,
                            behandlignstype,
                            melosysBehandlingstema,
                            expectedBehandlingstema,
                            expectedBehandlingstype,
                            expectedTema,
                            expectedOppgavetype,
                            forventetBegrunnelse ?: melosysBehandlingstema.beskrivelse
                        )
                    }
                }
            }
        }
    }

    private fun test(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        melosysBehandlingstema: Behandlingstema,
        expectedBehandlingstema: OppgaveBehandlingstema,
        expectedBehandlingstype: OppgaveBehandlingstype?,
        expectedTema: Tema,
        expectedOppgavetype: Oppgavetyper,
        forventetBegrunnelse: String
    ) {
        val behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                type = sakstype
                tema = sakstema

            }
            type = behandlingstype
            tema = melosysBehandlingstema
        }

        val oppgave = oppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()).build()

        withClue(
            "\nsakstype:               $sakstype " +
                "\nsakstema:               $sakstema " +
                "\nbehandlingstype:        $behandlingstype " +
                "\nmelosysBehandlingstema: $melosysBehandlingstema"
        ) {
            withClue("oppgave.behandlingstema") {
                oppgave.behandlingstema.shouldBe(expectedBehandlingstema.kode)
            }
            withClue("oppgave.behandlingstype") {
                oppgave.behandlingstype.shouldBe(expectedBehandlingstype?.kode)
            }
            withClue("oppgave.tema") {
                oppgave.tema.shouldBe(expectedTema)
            }
            withClue("oppgave.oppgavetype") {
                oppgave.oppgavetype.shouldBe(expectedOppgavetype)
            }
            withClue("oppgave.beskrivelse") {
                oppgave.beskrivelse.shouldBe(forventetBegrunnelse)
            }
        }
    }

}
