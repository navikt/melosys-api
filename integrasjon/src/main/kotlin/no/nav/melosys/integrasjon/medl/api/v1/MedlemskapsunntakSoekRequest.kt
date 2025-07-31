package no.nav.melosys.integrasjon.medl.api.v1

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

/**
 * Request DTO for søking etter medlemskapsunntak via MEDL API.
 *
 * Brukes med POST /rest/v1/periode/soek for å søke etter medlemskapsperioder
 * basert på ulike filterkriterier.
 *
 * @param personident Fødselsnummer, D-nummer eller aktørId for personen
 * @param type Type medlemskap: "MED_MEDLEMSKAP" eller "UTEN_MEDLEMSKAP"
 * @param statuser Liste over periodestatus-koder som skal inkluderes
 * @param fraOgMed Fra-og-med dato for søket (inklusiv)
 * @param tilOgMed Til-og-med dato for søket (inklusiv)
 * @param inkluderSporingsinfo Om sporingsinfo skal inkluderes i responsen
 * @param ekskluderKilder Liste over kilder som skal ekskluderes fra søket
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MedlemskapsunntakSoekRequest(
    val personident: String,
    val type: MedlemskapsunntakType? = null,
    val statuser: List<String>? = null,
    val fraOgMed: LocalDate? = null,
    val tilOgMed: LocalDate? = null,
    val inkluderSporingsinfo: Boolean? = null,
    val ekskluderKilder: List<String>? = null
)


enum class MedlemskapsunntakType(val verdi: String) {
    MED_MEDLEMSKAP("MED_MEDLEMSKAP"),
    UTEN_MEDLEMSKAP("UTEN_MEDLEMSKAP");

    companion object {
        fun fraVerdi(verdi: String): MedlemskapsunntakType? =
            values().find { it.verdi == verdi }
    }
}
