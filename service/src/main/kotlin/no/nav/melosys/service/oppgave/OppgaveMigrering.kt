package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.repository.BehandlingRepository
import org.springframework.stereotype.Component

@Component
class OppgaveMigrering(private val behandlingRepository: BehandlingRepository) {

    fun go() {
        println("kjører behandlingRepository.findAllByStatusNotIn")

        val sakOgBehandlinger = behandlingRepository.findSaksOgBehandlingTyperOgTeam(
            listOf(
                Behandlingsstatus.AVSLUTTET,
                Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING,
                Behandlingsstatus.IVERKSETTER_VEDTAK,
            )
        ).apply {
            println("size før erRedigerbar:${size}")
        }.filter { it.erRedigerbar() }

        println("sakOgBehandlinger filtrert${sakOgBehandlinger.size}")

        val first = sakOgBehandlinger.first()
        println(first)
    }
}
