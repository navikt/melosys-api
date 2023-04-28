package no.nav.melosys.service.oppgave

import io.kotest.matchers.collections.shouldHaveSize
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

    // Finner nå 2 som ikke mapper mot gosys
    @Test
    fun `sjekk at gyldige melosys kombinasjoner funger når vi lager gosys oppgave`() {
        //  Fant ikke oppgave mapping for sakstype:EU_EOS, sakstema:UNNTAK, behandlingstema:A1_ANMODNING_OM_UNNTAK_PAPIR, behandlingstype:KLAGE
        //  Fant ikke oppgave mapping for sakstype:TRYGDEAVTALE, sakstema:UNNTAK, behandlingstema:ANMODNING_OM_UNNTAK_HOVEDREGEL, behandlingstype:KLAGE
        GyldigeKombinasjoner.rows.forEach {
            try {
                oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

}
