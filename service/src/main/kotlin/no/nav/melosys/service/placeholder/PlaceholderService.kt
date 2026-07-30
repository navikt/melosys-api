package no.nav.melosys.service.placeholder

import no.nav.melosys.service.behandling.BehandlingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceholderService(
    private val behandlingService: BehandlingService,
    private val placeholderRegister: PlaceholderRegister,
) {

    fun hentKatalog(): List<PlaceholderDefinisjon> = placeholderRegister.definisjoner

    fun hentVerdier(behandlingId: Long): List<PlaceholderVerdi> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        return placeholderRegister.definisjoner.mapNotNull { definisjon ->
            runCatching { definisjon.resolver(behandling) }
                .onFailure { log.warn("Kunne ikke hente verdi for placeholder '{}', feltet utelates", definisjon.nokkel, it) }
                .getOrNull()
                ?.let { PlaceholderVerdi(nokkel = definisjon.nokkel, verdi = it) }
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PlaceholderService::class.java)
    }
}

data class PlaceholderVerdi(
    val nokkel: String,
    val verdi: String,
)
