package no.nav.melosys.service.placeholder

import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlaceholderService(
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade,
    private val placeholderRegister: PlaceholderRegister,
) {

    fun hentKatalog(): List<PlaceholderDefinisjon> = placeholderRegister.definisjoner

    @Transactional(readOnly = true)
    fun hentVerdier(behandlingId: Long): List<PlaceholderVerdi> {
        val kontekst = PlaceholderKontekst(behandlingService.hentBehandling(behandlingId)) { behandling ->
            persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID())
        }
        return placeholderRegister.definisjoner.mapNotNull { definisjon ->
            val verdi = try {
                definisjon.resolver(kontekst)
            } catch (e: Exception) {
                // Aldri verdier eller feilmeldinger i logg – de kan inneholde persondata
                log.warn(
                    "Kunne ikke hente verdi for placeholder '{}' ({}), feltet utelates",
                    definisjon.nokkel,
                    e.javaClass.simpleName,
                )
                null
            }
            // Kontrakten lover at en manglende verdi utelates, aldri leveres som tom streng
            verdi?.takeIf { it.isNotBlank() }?.let { PlaceholderVerdi(nokkel = definisjon.nokkel, verdi = it) }
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
