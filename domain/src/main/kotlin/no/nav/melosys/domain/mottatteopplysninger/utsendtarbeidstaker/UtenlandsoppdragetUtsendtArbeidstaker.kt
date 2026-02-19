package no.nav.melosys.domain.mottatteopplysninger.utsendtarbeidstaker

import no.nav.melosys.domain.mottatteopplysninger.data.Periode

data class UtenlandsoppdragetUtsendtArbeidstaker(
    val samletUtsendingsperiode: Periode = Periode(),
    val erUtsendelseForOppdragIUtlandet: Boolean? = null,
    val erFortsattAnsattEtterOppdraget: Boolean? = null,
    val erAnsattForOppdragIUtlandet: Boolean? = null,
    val erDrattPaaEgetInitiativ: Boolean? = null,
    val erErstatningTidligereUtsendte: Boolean? = null
)
