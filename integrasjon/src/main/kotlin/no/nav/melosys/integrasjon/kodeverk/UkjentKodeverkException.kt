package no.nav.melosys.integrasjon.kodeverk

class UkjentKodeverkException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Exception?) : super(message, cause)
}
