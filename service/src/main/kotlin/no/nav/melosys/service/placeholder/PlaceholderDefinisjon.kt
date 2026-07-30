package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.person.Persondata

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
 * Sakskonteksten resolverne får. Persondata hentes ved første bruk og gjenbrukes, slik at ett
 * oppslag dekker alle persondatafeltene. Feiler oppslaget, kastes samme feil til hver resolver.
 */
class PlaceholderKontekst(
    val behandling: Behandling,
    persondataOppslag: (Behandling) -> Persondata,
) {
    // Fanger Exception og ikke Throwable, slik at Error får velte kallet
    private val persondataResultat: Result<Persondata> by lazy {
        try {
            Result.success(persondataOppslag(behandling))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val persondata: Persondata get() = persondataResultat.getOrThrow()
}
