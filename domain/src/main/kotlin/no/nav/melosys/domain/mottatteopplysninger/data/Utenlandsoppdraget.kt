package no.nav.melosys.domain.mottatteopplysninger.data


data class Utenlandsoppdraget(
    var samletUtsendingsperiode: Periode? = null,
    var erUtsendelseForOppdragIUtlandet: Boolean? = null,
    var erFortsattAnsattEtterOppdraget: Boolean? = null,
    var erAnsattForOppdragIUtlandet: Boolean? = null,
    var erDrattPaaEgetInitiativ: Boolean? = null,
    var erErstatningTidligereUtsendte: Boolean? = null
)
