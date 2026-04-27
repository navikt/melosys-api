package no.nav.melosys.domain.avgift

enum class Avgiftsberegningsregel {
    ORDINÆR,
    TJUEFEM_PROSENT_REGEL,
    MINSTEBELØP;

    companion object {
        /**
         * Tolerant parsing: upstream-versjoner før norske tegn-oppdateringen sendte ASCII-aliaser
         * (ORDINAER, MINSTEBELOEP). Faller tilbake til [valueOf] med rå verdi for å beholde
         * den opprinnelige feilmeldingen ved ukjent navn.
         */
        fun parse(verdi: String): Avgiftsberegningsregel {
            val normalisert = verdi.uppercase()
                .replace("AE", "Æ")
                .replace("OE", "Ø")
            return entries.firstOrNull { it.name == normalisert }
                ?: valueOf(verdi)
        }
    }
}
