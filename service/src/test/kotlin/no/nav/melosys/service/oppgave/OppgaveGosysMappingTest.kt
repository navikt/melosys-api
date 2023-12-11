package no.nav.melosys.service.oppgave

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.lovligekombinasjoner.GyldigeKombinasjoner
import org.junit.jupiter.api.Test

class OppgaveGosysMappingTest {

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
    fun `sjekk at gyldige melosys kombinasjoner funger når vi lager gosys oppgave`() {
        GyldigeKombinasjoner.rowsMelosysOgDatavarehus.forEach {
            // https://confluence.adeo.no/display/TEESSI/Alle+kombinasjoner+fra+melosys+og+dvh+med+mapping+til+gosys+oppgave
            oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
        }
    }

    @Test
    fun `Skal ha 3 stk med Beskrivelsefelt SED`() {
        oppgaveGosysMapping.rows
            .filter { it.oppgave.beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.SED }
            .shouldHaveSize(3)
            .map { it.oppgave.oppgaveBehandlingstema?.kode }
            .shouldContainAll("ab0482", "ab0490", "ab0491")
    }

    @Test
    fun `kombo TRYGDEAVTALE,  UNNTAK, HENVENDELSE, FORESPØRSEL_TRYGDEMYNDIGHET skal gi tema Unntak fra medlemskap`() {
        val oppgave = oppgaveGosysMapping.finnOppgave(
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            Behandlingstyper.HENVENDELSE
        )

        oppgave.apply {
            oppgaveBehandlingstema.shouldBeNull()
            tema.shouldBe(Tema.UFM)
            oppgaveType.shouldBe(Oppgavetyper.BEH_SAK_MK)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.TOMT)
        }
    }

    @Test
    fun `kombo FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, MANGLENDE_INNBETALING_TRYGDEAVGIFT skal gi oppgavetype VURD_MAN_INNB`() {
        val oppgave = oppgaveGosysMapping.finnOppgave(
            Sakstyper.FTRL,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )

        oppgave.apply {
            oppgaveBehandlingstema.shouldBe(OppgaveBehandlingstema.UTENFOR_AVTALAND_YRKESAKTIV)
            tema.shouldBe(Tema.MED)
            oppgaveType.shouldBe(Oppgavetyper.VURD_MAN_INNB)
            beskrivelsefelt.shouldBe(OppgaveGosysMapping.Beskrivelsefelt.TOMT)
        }
    }
}
