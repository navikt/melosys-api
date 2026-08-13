package no.nav.melosys.saksflytapi.domain

import java.util.regex.Pattern

private const val UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"

enum class LåsReferanseType(val prefixRegexString: String) {
    SED("^\\d+_[a-zA-Z0-9]+_\\d+$"),
    UBETALT("^UBETALT_.\\w+_\\d+\$"),

    // Digital søknad: "{gruppeId}_{skjemaId}" — gruppeId er gruppePrefiks (serialisering per
    // søknadsgruppe), skjemaId gjør referansen unik per del (redelivery-dedup). Begge er UUID-er.
    //
    // gruppeId-delen er valgfri i regexen fordi det gamle formatet var en bar skjemaId-UUID.
    // Prosessinstanser opprettet før MELOSYS-8151 lever videre i databasen med det formatet, og
    // ProsessinstansFerdigListener parser låsreferansen til ALLE prosessinstanser på vent ved hvert
    // ferdig-event. Uten toleranse her ville én gammel rad kastet IllegalArgumentException og
    // stanset opplåsingen av alle ventende prosesser — også for andre prosesstyper.
    SØKNAD("^$UUID_REGEX(_$UUID_REGEX)?\$");

    fun erGyldigReferanse(referanse: String): Boolean =
        Pattern.compile(prefixRegexString).matcher(referanse).find()
}
