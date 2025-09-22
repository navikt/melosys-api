package no.nav.melosys.domain.person

enum class KjoennType(val kode: String) {
    KVINNE("K"),
    MANN("M"),
    UKJENT("U");

    companion object {
        fun avKode(kode: String?): KjoennType =
            entries.find { it.kode == kode } ?: UKJENT
    }
}
