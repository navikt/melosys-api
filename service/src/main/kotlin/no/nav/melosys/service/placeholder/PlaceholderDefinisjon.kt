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
    val resolver: (PlaceholderKontekst) -> PlaceholderResultat?,
)

/**
 * Verdien er forhåndsvalget, og kandidatlisten inneholder den. Ett eller ingen alternativ betyr
 * at feltet ikke er flertydig – servicen utelater da kandidatene fra svaret, som kontrakten krever.
 */
data class PlaceholderResultat(
    val verdi: String,
    val kandidater: List<String> = emptyList(),
)

/**
 * Sakskonteksten resolverne får. Sakens egne felter er lest ut av databasen på forhånd, mens
 * persondata og navnene på norske arbeidsgivere hentes ved første bruk og gjenbrukes, slik at ett
 * oppslag dekker alle feltene som trenger det. Feiler oppslaget, kastes samme feil til hver resolver.
 */
class PlaceholderKontekst(
    val sakskontekst: PlaceholderSakskontekst,
    val landnavn: (String?) -> String,
    persondataOppslag: (String) -> Persondata,
    norskeArbeidsgivernavnOppslag: (Set<String>) -> List<String>,
) {
    val saksnummer: String get() = sakskontekst.saksnummer

    // Fanger Exception og ikke Throwable, slik at Error får velte kallet
    private val persondataResultat: Result<Persondata> by lazy {
        try {
            val aktørID = sakskontekst.brukersAktørID
                ?: throw FunksjonellException("Finner ikke bruker på fagsak $saksnummer")
            Result.success(persondataOppslag(aktørID))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Egen lazy for EREG: HTTP-kallet skjer kun når {arbeidsgiver-norge} faktisk er i bruk
    private val norskeArbeidsgivernavnResultat: Result<List<String>> by lazy {
        val orgnumre = sakskontekst.norskeArbeidsgivereOrgnumre
        try {
            Result.success(if (orgnumre.isEmpty()) emptyList() else norskeArbeidsgivernavnOppslag(orgnumre))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val persondata: Persondata get() = persondataResultat.getOrThrow()

    val norskeArbeidsgivernavn: List<String> get() = norskeArbeidsgivernavnResultat.getOrThrow()
}
