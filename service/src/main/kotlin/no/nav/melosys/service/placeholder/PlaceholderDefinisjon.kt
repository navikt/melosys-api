package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.exception.FunksjonellException

/**
 * Én placeholder i registeret. Tom sakstyper-liste betyr at feltet gjelder alle sakstyper.
 * Eksempelet er en funksjon fordi noen felter (dagens dato) må beregnes ved hvert oppslag.
 */
data class PlaceholderDefinisjon(
    val nokkel: String,
    val visningsnavn: String,
    val beskrivelse: String,
    val eksempel: () -> String,
    val sakstyper: List<Sakstyper> = emptyList(),
    val resolver: (PlaceholderKontekst) -> String?,
)

/**
 * Sakskonteksten resolverne får. Sakens egne felter er lest ut av databasen på forhånd, mens
 * persondata hentes ved første bruk og gjenbrukes, slik at ett oppslag dekker alle
 * persondatafeltene. Feiler oppslaget, kastes samme feil til hver resolver.
 */
class PlaceholderKontekst(
    val saksnummer: String,
    brukersAktørID: String?,
    persondataOppslag: (String) -> Persondata,
) {
    // Fanger Exception og ikke Throwable, slik at Error får velte kallet
    private val persondataResultat: Result<Persondata> by lazy {
        try {
            val aktørID = brukersAktørID ?: throw FunksjonellException("Finner ikke bruker på fagsak $saksnummer")
            Result.success(persondataOppslag(aktørID))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val persondata: Persondata get() = persondataResultat.getOrThrow()
}
