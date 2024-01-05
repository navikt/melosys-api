package no.nav.melosys.saksflytapi.domain

import java.util.regex.Pattern

enum class LåsReferanseType(val prefixRegexString: String) {
    SED("^\\d+_[a-zA-Z0-9]+_\\d+$"),
    UBETALT("^UBETALT_.\\w+_\\d+\$");

    fun erGyldigReferanse(referanse: String): Boolean =
        Pattern.compile(prefixRegexString).matcher(referanse).find()
}
