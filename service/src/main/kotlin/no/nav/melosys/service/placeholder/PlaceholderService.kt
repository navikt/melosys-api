package no.nav.melosys.service.placeholder

import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.IdentityHashMap

@Service
class PlaceholderService(
    private val sakskontekstHenter: PlaceholderSakskontekstHenter,
    private val persondataFasade: PersondataFasade,
    private val organisasjonOppslagService: OrganisasjonOppslagService,
    private val landnavnOppslag: PlaceholderLandnavnOppslag,
    private val placeholderRegister: PlaceholderRegister,
) {

    fun hentKatalog(): PlaceholderKatalog =
        PlaceholderKatalog(placeholderRegister.definisjoner, placeholderRegister.betingelser)

    /**
     * Uten transaksjon: DB-tilkoblingen er sluppet før persondata og arbeidsgivernavn hentes, slik at
     * et tregt PDL- eller EREG-kall ikke legger beslag på en tilkobling fra poolen.
     */
    fun hentVerdier(behandlingId: Long): PlaceholderVerdier {
        val kontekst = PlaceholderKontekst(
            sakskontekst = sakskontekstHenter.hent(behandlingId),
            landnavn = landnavnOppslag::landnavn,
            persondataOppslag = { aktørID -> persondataFasade.hentPerson(aktørID) },
            // EREG svarer med et sett – sorteres for at kandidatlisten skal være stabil mellom oppslag
            norskeArbeidsgivernavnOppslag = { orgnumre ->
                organisasjonOppslagService.hentOrganisasjoner(orgnumre).map { it.navn }.sorted()
            },
        )
        // Samme cachede feil kastes til hvert persondatafelt – samles her og logges én gang per feil
        val utelatteNokler = IdentityHashMap<Exception, MutableList<String>>()
        val verdier = placeholderRegister.definisjoner.mapNotNull { definisjon ->
            utelatVedFeil(utelatteNokler, definisjon.nokkel) { definisjon.resolver(kontekst) }
                ?.let { tilVerdi(definisjon.nokkel, it) }
        }
        // En betingelse uten tilgjengelig faktum utelates helt, slik at web beholder innholdet uerstattet
        val betingelser = placeholderRegister.betingelser
            .filter { it.gjelderFor(kontekst.sakskontekst.sakstype) }
            .mapNotNull { betingelse ->
                utelatVedFeil(utelatteNokler, betingelse.nokkel) { betingelse.vurdering(kontekst) }
                    ?.let { BetingelseVerdi(betingelse.nokkel, it) }
            }
        utelatteNokler.forEach { (feil, nokler) -> loggUtelatteFelt(nokler, feil) }
        return PlaceholderVerdier(verdier, betingelser)
    }

    private fun <T> utelatVedFeil(
        utelatteNokler: IdentityHashMap<Exception, MutableList<String>>,
        nokkel: String,
        oppslag: () -> T?,
    ): T? = try {
        oppslag()
    } catch (e: Exception) {
        utelatteNokler.getOrPut(e) { mutableListOf() } += nokkel
        null
    }

    /**
     * Kontrakten lover at en manglende verdi utelates, aldri leveres som tom streng, og at
     * kandidatene kun er med når det finnes reelt flere å velge mellom.
     */
    private fun tilVerdi(nokkel: String, resultat: PlaceholderResultat): PlaceholderVerdi? {
        val verdi = resultat.verdi.trim().takeIf { it.isNotEmpty() } ?: return null
        val kandidater = resultat.kandidater.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return PlaceholderVerdi(nokkel = nokkel, verdi = verdi, kandidater = kandidater.takeIf { it.size > 1 })
    }

    // Nøklene er trygge å logge, men aldri verdier eller feilmeldinger – de kan inneholde persondata
    private fun loggUtelatteFelt(nokler: List<String>, e: Exception) {
        val melding = "Kunne ikke slå opp nøklene [{}] ({}), feltene utelates"
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

data class PlaceholderKatalog(
    val placeholdere: List<PlaceholderDefinisjon>,
    val betingelser: List<BetingelseDefinisjon>,
)

data class PlaceholderVerdier(
    val verdier: List<PlaceholderVerdi>,
    val betingelser: List<BetingelseVerdi>,
)

data class PlaceholderVerdi(
    val nokkel: String,
    val verdi: String,
    val kandidater: List<String>? = null,
)

data class BetingelseVerdi(
    val nokkel: String,
    val oppfylt: Boolean,
)
