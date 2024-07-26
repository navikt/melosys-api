package no.nav.melosys.service.oppgave.migrering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OppgaveIdMigrering(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveFasade: OppgaveFasade,
    private val migreringsRapport: OppgaveIdMigreringRapport
) {
    @Volatile
    var stopMigrering: Boolean = false


    fun status(): Map<String, Any> {
        return migreringsRapport.status()
    }


    @Async
    @Synchronized
    fun go(dryrun: Boolean) {
        ThreadLocalAccessInfo.executeProcess("Migrer oppgaveIder") {
            migrering(dryrun)
        }
    }


    @Async
    @Synchronized
    fun migrerOppgaveId(dryrun: Boolean) {
        ThreadLocalAccessInfo.executeProcess("Migrer oppgaveIder") {
            migrering(dryrun)
        }
    }

    fun stop() {
        stopMigrering = true
        log.info("Migrering stoppet!")
    }

    private fun finnÅpneOppgaver(): MutableCollection<Oppgave> {
        return oppgaveFasade.finnÅpneBehandlingsoppgaver()
    }

    internal fun migrering(
        dryrun: Boolean,
        options: Options = Options()
    ) {
        log.info(
            "Utfører OppgaveMigrering. \n" +
                options.toJsonNode.toPrettyString()
        )
        finnÅpneOppgaver().apply {
            migreringsRapport.antallOppgaverFunnet = size
        }.asSequence()
            .filter { !stopMigrering }
            .filter {
                !migreringsRapport.harOppgaveIdBlittMigrert(it.oppgaveId)
            }
            .forEach {
                // Oppdater oppgave
                val fagsak = fagsakRepository.findBySaksnummer(it.saksnummer)
                if (fagsak.isEmpty) {
                    migreringsRapport.oppgaveMigrert(it, null, null)

                    return@forEach
                }

                val åpneBehandlinger = fagsak.get().behandlinger.filter { it.erAktiv() }
                if (åpneBehandlinger.isEmpty()) {
                    migreringsRapport.oppgaveMigrert(it, it.saksnummer, null)
                } else if (åpneBehandlinger.size > 1) {
                    migreringsRapport.oppgaveMigrert(it, it.saksnummer, null, true)
                } else {
                    val behandling = åpneBehandlinger.first()
                    if (!dryrun) {
                        behandling.oppgaveId = it.oppgaveId
                        behandlingRepository.save(behandling)
                    }
                    migreringsRapport.oppgaveMigrert(it, it.saksnummer, behandling.id)
                    migreringsRapport.antallSakerMigrert++
                }
            }

        log.info("OppgaveMigrering utført! ${if (stopMigrering) "Stoppet manuelt!" else ""}")
        lagRapport()
    }


    private fun lagRapport() {
        val status = migreringsRapport.statusEtterKjøring()
        log.info(status)
        migreringsRapport.saveStatusFiles(status)
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }


    data class Options(
        val migrerOppgaver: Boolean = true,
    )


}
