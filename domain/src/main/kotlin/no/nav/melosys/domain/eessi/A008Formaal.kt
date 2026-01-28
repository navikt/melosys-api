package no.nav.melosys.domain.eessi

/**
 * Formål med A008 SED i CDM 4.4.
 *
 * Definerer gyldige verdier for a008Formaal-feltet som sendes til RINA.
 */
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
