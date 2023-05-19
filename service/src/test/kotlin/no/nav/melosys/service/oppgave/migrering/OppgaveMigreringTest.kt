package no.nav.melosys.service.oppgave.migrering

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.BehandlingRepositoryForOppgaveMigrering
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.core.env.StandardEnvironment
import java.io.File

@Disabled("brukes bare for å lage oppgave migrering html rapporter, fjerns fra git etter at migreing er utført")
class OppgaveMigreringTest {
    private val sedTyper = SedType.values().map { it.name }.toSet()

    @Test
    fun test() {
        val migreringsRapport = Migrering.migreringsRapportFraJson("/Users/rune/div/jsonrapport-prod.json")

        File("/Users/rune/div/migrerings-rapport.html").writeText(migreringsRapport.html { migreringsSaker ->
            migreringsSaker
                .filter { !it.ny.fantIkkeOppgaveMapping() }
                .filter { it.oppgaver.size == 1 }
                .filter { sedTyper.contains(it.ny.beskrivelse) }
        })

    }

    @Test
    fun `kjør migreing fra tidligere jsonrapport fil`() {
        val migreringsListe =
            Migrering.migreringsRapportFraJson("/Users/rune/div/jsonrapport-prod.json").sortedMigreringsListe()

        val behandlingRepository = mockk<BehandlingRepositoryForOppgaveMigrering>()
        every { behandlingRepository.finnSaksOgBehandlingTyperOgTema(any()) } returns migreringsListe.map { it.sak }

        val oppgaveFasade = mockk<OppgaveFasade>()
        migreringsListe.groupBy { it.sak.saksnummer }
            .map { it.key to it.value.map { o -> o.oppgaver }.firstOrNull() }
            .forEach { (sak, oppgaver) ->
                every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(sak) } returns oppgaver
            }
        migreringsListe.forEach { migreringsSak ->
            every { behandlingRepository.findWithSaksopplysningerById(migreringsSak.sak.behandlingID) } returns lagBehandling(
                migreringsSak
            )
        }

        OppgaveMigrering(
            behandlingRepository,
            oppgaveFasade,
            MigreringsRapport(StandardEnvironment())
        ).migrering(null, null, true)
    }

    private fun lagBehandling(
        migreringsSak: MigreringsSak
    ): Behandling {
        val sak = migreringsSak.sak
        val sakstype: Sakstyper = sak.sakstype
        val sakstema: Sakstemaer = sak.sakstema
        val behandlingstema: Behandlingstema = sak.behandlingstema
        val behandlingstype: Behandlingstyper = sak.behandlingstype
        val sedName = sedTyper.find { it == migreringsSak.ny.beskrivelse }

        return Behandling().apply {
            id = 1
            fagsak = Fagsak().apply {
                saksnummer = "MEL-1"
                type = sakstype
                tema = sakstema
            }
            type = behandlingstype
            tema = behandlingstema
            if (sedName != null) {
                saksopplysninger.add(Saksopplysning().apply {
                    type = SaksopplysningType.SEDOPPL
                    dokument = SedDokument().apply {
                        sedType = SedType.valueOf(sedName)
                    }
                })
            }
        }
    }
}
