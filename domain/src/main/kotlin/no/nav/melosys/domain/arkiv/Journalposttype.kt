package no.nav.melosys.domain.arkiv

enum class Journalposttype(private val kode: String) {
    /**
     * Inngående dokument
     */
    INN("I"),

    /**
     * Utgående dokument
     */
    UT("U"),

    /**
     * Internt notat
     */
    NOTAT("N");

    fun getKode(): String = kode
}
