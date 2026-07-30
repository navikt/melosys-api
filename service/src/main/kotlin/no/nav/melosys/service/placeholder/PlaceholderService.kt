package no.nav.melosys.service.placeholder

import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Collections
import java.util.IdentityHashMap

@Service
class PlaceholderService(
    private val sakskontekstHenter: PlaceholderSakskontekstHenter,
    private val persondataFasade: PersondataFasade,
    private val placeholderRegister: PlaceholderRegister,
) {

    fun hentKatalog(): List<PlaceholderDefinisjon> = placeholderRegister.definisjoner

    /**
     * Uten transaksjon: DB-tilkoblingen er sluppet før persondata hentes, slik at et tregt
     * PDL-kall ikke legger beslag på en tilkobling fra poolen.
     */
    fun hentVerdier(behandlingId: Long): List<PlaceholderVerdi> {
        val sakskontekst = sakskontekstHenter.hent(behandlingId)
        val kontekst = PlaceholderKontekst(sakskontekst.saksnummer, sakskontekst.brukersAktørID) { aktørID ->
            persondataFasade.hentPerson(aktørID)
        }
        // Samme cachede feil kastes til hvert persondatafelt – logg den kun én gang per kall
        val loggedeFeil = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        return placeholderRegister.definisjoner.mapNotNull { definisjon ->
            val verdi = try {
                definisjon.resolver(kontekst)
            } catch (e: Exception) {
                if (loggedeFeil.add(e)) loggUtelattFelt(definisjon.nokkel, e)
                null
            }
            // Kontrakten lover at en manglende verdi utelates, aldri leveres som tom streng
            verdi?.trim()?.takeIf { it.isNotEmpty() }?.let { PlaceholderVerdi(nokkel = definisjon.nokkel, verdi = it) }
        }
    }

    // Aldri verdier eller feilmeldinger i logg – de kan inneholde persondata
    private fun loggUtelattFelt(nokkel: String, e: Exception) {
        val melding = "Kunne ikke hente verdi for placeholder '{}' ({}), feltet utelates"
        // Fagsak uten bruker er en forventet tilstand, ikke en driftsfeil
        if (e is FunksjonellException) {
            log.info(melding, nokkel, e.javaClass.simpleName)
        } else {
            log.warn(melding, nokkel, e.javaClass.simpleName)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PlaceholderService::class.java)
    }
}

/**
 * Egen bean slik at transaksjonen faktisk lukkes før persondataoppslaget: et internt kall
 * i PlaceholderService ville gått utenom Spring-proxyen.
 */
@Component
class PlaceholderSakskontekstHenter(
    private val behandlingService: BehandlingService,
) {

    @Transactional(readOnly = true)
    fun hent(behandlingId: Long): PlaceholderSakskontekst {
        val fagsak = behandlingService.hentBehandling(behandlingId).fagsak
        // Begge feltene må leses her – fagsaken er en lazy proxy og transaksjonen er over etterpå
        return PlaceholderSakskontekst(
            saksnummer = fagsak.saksnummer,
            brukersAktørID = fagsak.finnBrukersAktørID(),
        )
    }
}

data class PlaceholderSakskontekst(
    val saksnummer: String,
    val brukersAktørID: String?,
)

data class PlaceholderVerdi(
    val nokkel: String,
    val verdi: String,
)
