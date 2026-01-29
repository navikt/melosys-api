package no.nav.melosys.domain.eessi

import mu.KotlinLogging
import no.nav.melosys.exception.FunksjonellException

/**
 * Formål med A008 SED i CDM 4.4.
 *
 * Definerer gyldige verdier for a008Formaal-feltet som sendes til RINA.
 */
private val log = KotlinLogging.logger { }

enum class A008Formaal(val verdi: String) {
    /**
     * Endringsmelding - brukes når det er endringer i eksisterende lovvalg
     */
    ENDRINGSMELDING("endringsmelding"),

    /**
     * Arbeid i flere land - brukes ved informasjon om arbeid i to eller flere medlemsland
     */
    ARBEID_FLERE_LAND("arbeid_flere_land");

    companion object {
        private val verdierMap = entries.associateBy { it.verdi }

        /**
         * Finner A008Formaal fra string-verdi.
         * @param verdi String-verdien som skal konverteres
         * @return A008Formaal hvis verdien er gyldig, null ellers
         */
        @JvmStatic
        fun fraVerdi(verdi: String?): A008Formaal? = verdi?.let { verdierMap[it] }

        @JvmStatic
        fun hentVerdi(verdi: String?): A008Formaal = fraVerdi(verdi)
            ?: let {
                log.warn("Ugyldig verdi for a008Formaal: '$verdi'. Gyldige verdier er: ${gyldigeVerdier()}")
                ARBEID_FLERE_LAND // i dag så er denne default ved CDM 4.4
            }

        /**
         * Validerer at en string-verdi er en gyldig A008Formaal.
         * @param verdi String-verdien som skal valideres
         * @return true hvis verdien er gyldig eller null, false ellers
         */
        @JvmStatic
        fun erGyldig(verdi: String?): Boolean = verdi == null || verdierMap.containsKey(verdi)

        /**
         * Returnerer liste over alle gyldige verdier som strings.
         */
        @JvmStatic
        fun gyldigeVerdier(): List<String> = entries.map { it.verdi }
    }
}
