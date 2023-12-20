package no.nav.melosys.saksflytapi.domain

import java.util.regex.Pattern

enum class LåsReferanseType(val prefixRegexString: String) {
    SED("^\\d+_[a-zA-Z0-9]+_\\d+$"),
    OMIB("^OMIB_.\\w+_\\d+\$");

    fun erGyldigReferanse(referanse: String?): Boolean =
        referanse != null && Pattern.compile(prefixRegexString).matcher(referanse).find()
}
