package no.nav.melosys.service.oppgave.migrering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.repository.BehandlingRepositoryForOppgaveMigrering
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.core.env.StandardEnvironment
import java.io.File

class OppgaveMigreringTest {
    private val sedTyper = SedType.values().map { it.name }.toSet()

    @Test
    fun `sjekk at vi ikke migrer allerede migrerte`() {
        val migreringsListe = listOf<MigreringsSak>(
            MigreringsSak(
                sak = SakOgBehandlingDTO(
                    saksnummer = "MEL-1",
                    behandlingID = 1L,
                    sakstype = Sakstyper.EU_EOS,
                    sakstema = Sakstemaer.UNNTAK,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                    behandlingstatus = Behandlingsstatus.VURDER_DOKUMENT
                ),
                oppgaver = listOf(
                    Oppgave.Builder()
                        .setOppgaveId("1")
                        .setBehandlingstema("ab0490")
                        .setOppgavetype(Oppgavetyper.BEH_SED)
                        .setTema(Tema.UFM)
                        .build()
                ),
                ny = OppgaveMigreringsOppdatering(
                    oppgaveBehandlingstema = OppgaveBehandlingstema.EU_EOS_MELDING_OM_UNNTAK_UNNTAK,
                    oppgaveBehandlingstype = null,
                    tema = Tema.UFM,
                    oppgaveType = Oppgavetyper.BEH_SED,
                    beskrivelse = ""
                )
            )
        )

        val behandlingRepository = mockk<BehandlingRepositoryForOppgaveMigrering>()
        every { behandlingRepository.finnSaksOgBehandlingTyperOgTema(any()) } returns migreringsListe.map { it.sak }

        val oppgaveFasade = mockk<OppgaveFasade>()

        val migreringsSak = migreringsListe.first()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer("MEL-1") } returns migreringsSak.oppgaver
        every { behandlingRepository.findWithSaksopplysningerById(migreringsSak.sak.behandlingID) } returns lagBehandling(
            migreringsSak
        )

        val migreringsRapport = MigreringsRapport(StandardEnvironment())
        OppgaveMigrering(
            behandlingRepository,
            oppgaveFasade,
            migreringsRapport
        ).migrering(null, null, false)

        verify(exactly = 0) { oppgaveFasade.oppdaterOppgave(any(), any()) }
        migreringsRapport.status()["alleredeMigrert"].shouldBe(1)
    }

    @Test
    @Disabled("brukes bare for å lage oppgave migrering html rapporter, fjerns fra git etter at migreing er utført")
    fun `lag rapport`() {
        val migreringsRapport =
            Migrering.migreringsRapportFraJson("/Users/rune/div/dryrun-0520/jsonrapport-prod-0520.json")

        File("/Users/rune/div/migrerings-rapport.html").writeText(migreringsRapport.html { migreringsSaker ->
            migreringsSaker
                .filter { it.sak.sakstype == Sakstyper.EU_EOS }
                .filter { it.sak.sakstema == Sakstemaer.UNNTAK }
                .filter { it.sak.behandlingstype == Behandlingstyper.HENVENDELSE }
                .filter { it.sak.behandlingstema == Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND }
                .filter { it.oppgaver.size == 1 }
        })
    }

    @Test
    @Disabled("brukes bare for å lage oppgave migrering html rapporter, fjerns fra git etter at migreing er utført")
    fun `kjør migreing fra tidligere jsonrapport fil`() {
        val migreringsListe =
            Migrering.migreringsRapportFraJson("/Users/rune/div/dryrun-0520/jsonrapport-prod-0520.json")
                .sortedMigreringsListe()

        val behandlingRepository = mockk<BehandlingRepositoryForOppgaveMigrering>()
        every { behandlingRepository.finnSaksOgBehandlingTyperOgTema(any()) } returns migreringsListe.map { it.sak }

        val oppgaveFasade = mockk<OppgaveFasade>()

        every { oppgaveFasade.oppdaterOppgave(any(), any()) } answers {
            val id = firstArg<String>()
            val oppgaveOppdatering = secondArg<OppgaveOppdatering>()
            if (id == "361544672") throw IllegalStateException("Feil ved oppdatering av oppgave 361544672")
            if (oppgaveOppdatering.beskrivelse != "") {
                throw IllegalStateException("Beskrivelse skal være tom")
            } else Unit
        }

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

        val migreringsRapport = MigreringsRapport(StandardEnvironment())
        OppgaveMigrering(
            behandlingRepository,
            oppgaveFasade,
            migreringsRapport
        ).migrering(null, null, false)

        migreringsRapport.status()["migreringFeilet"].shouldBe(1)
        migreringsRapport.sortedMigreringsListe().filter {
            it.ny.oppgaveOppdateringError != null
        }.shouldHaveSize(1)
            .first().ny.oppgaveOppdateringError.shouldBe("Feil ved oppdatering av oppgave 361544672")
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
