package no.nav.melosys.service.placeholder

import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
        // Samme cachede feil kastes til hvert persondatafelt – samles her og logges én gang per feil
        val utelatteNokler = IdentityHashMap<Exception, MutableList<String>>()
        val verdier = placeholderRegister.definisjoner.mapNotNull { definisjon ->
            val verdi = try {
                definisjon.resolver(kontekst)
            } catch (e: Exception) {
                utelatteNokler.getOrPut(e) { mutableListOf() } += definisjon.nokkel
                null
            }
            // Kontrakten lover at en manglende verdi utelates, aldri leveres som tom streng
            verdi?.trim()?.takeIf { it.isNotEmpty() }?.let { PlaceholderVerdi(nokkel = definisjon.nokkel, verdi = it) }
        }
        utelatteNokler.forEach { (feil, nokler) -> loggUtelatteFelt(nokler, feil) }
        return verdier
    }

    // Nøklene er trygge å logge, men aldri verdier eller feilmeldinger – de kan inneholde persondata
    private fun loggUtelatteFelt(nokler: List<String>, e: Exception) {
        val melding = "Kunne ikke hente verdi for placeholderne [{}] ({}), feltene utelates"
        val nokkelListe = nokler.joinToString()
        // Fagsak uten bruker er en forventet tilstand, ikke en driftsfeil
        if (e is FunksjonellException) {
            log.info(melding, nokkelListe, e.javaClass.simpleName)
        } else {
            log.warn(melding, nokkelListe, e.javaClass.simpleName)
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
