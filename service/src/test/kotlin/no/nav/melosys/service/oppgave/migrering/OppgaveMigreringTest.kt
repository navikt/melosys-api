package no.nav.melosys.service.oppgave.migrering

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class OppgaveMigreringTest {
    @Test
    @Disabled("brukes bare for å lage oppgave migrering html rapporter, fjerns fra git etter at migreing er utført")
    fun test() {
        val migreringsRapport = Migrering.migreringsRapportFraJson("/Users/rune/div/jsonrapport-prod.json")
        File("/Users/rune/div/migrerings-rapport.html").writeText(migreringsRapport.html { migreringsSaker ->
            migreringsSaker
                .filter { it.harFeil() }
        })
    }
}
