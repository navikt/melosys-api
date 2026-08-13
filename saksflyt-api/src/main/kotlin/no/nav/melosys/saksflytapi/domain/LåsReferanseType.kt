package no.nav.melosys.saksflytapi.domain

import java.util.regex.Pattern

enum class LåsReferanseType(val prefixRegexString: String) {
    SED("^\\d+_[a-zA-Z0-9]+_\\d+$"),
    UBETALT("^UBETALT_.\\w+_\\d+\$"),
    // Digital søknad: "{gruppeId}_{skjemaId}" — gruppeId er gruppePrefiks (serialisering per
    // søknadsgruppe), skjemaId gjør referansen unik per del (redelivery-dedup). Begge er UUID-er.
    SØKNAD("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$");

    fun erGyldigReferanse(referanse: String): Boolean =
        Pattern.compile(prefixRegexString).matcher(referanse).find()
}
