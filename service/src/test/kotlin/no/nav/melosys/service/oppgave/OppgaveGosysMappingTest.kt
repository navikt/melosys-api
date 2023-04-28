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

    @Test
    fun `sjekk at gyldige melosys kombinasjoner funger når vi lager gosys oppgave`() {
        GyldigeKombinasjoner.rows.forEach {
            oppgaveGosysMapping.finnOppgave(it.sakstype, it.sakstema, it.behandlingstema, it.behandlingstype)
        }
    }

}
