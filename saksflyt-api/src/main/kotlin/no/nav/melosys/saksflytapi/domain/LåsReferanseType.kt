package no.nav.melosys.saksflytapi.domain

import java.util.regex.Pattern

private const val UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"

enum class LåsReferanseType(val prefixRegexString: String) {
    SED("^\\d+_[a-zA-Z0-9]+_\\d+$"),
    UBETALT("^UBETALT_.\\w+_\\d+\$"),
    // "{skjemaId}" (gammelt format) eller "{gruppe}_{skjemaId}", der gruppe er skjemaId-en som startet gruppen
    SØKNAD("^$UUID_REGEX(_$UUID_REGEX)?\$");

    fun erGyldigReferanse(referanse: String): Boolean =
        Pattern.compile(prefixRegexString).matcher(referanse).find()
}
