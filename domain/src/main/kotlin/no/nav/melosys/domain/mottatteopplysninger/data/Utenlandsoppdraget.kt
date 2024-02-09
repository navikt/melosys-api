package no.nav.melosys.domain.mottatteopplysninger.data


class Utenlandsoppdraget {
    var samletUtsendingsperiode = Periode()
    var erUtsendelseForOppdragIUtlandet: Boolean? = null
    var erFortsattAnsattEtterOppdraget: Boolean? = null
    var erAnsattForOppdragIUtlandet: Boolean? = null
    var erDrattPaaEgetInitiativ: Boolean? = null
    var erErstatningTidligereUtsendte: Boolean? = null

    constructor()
    constructor(
        samletUtsendingsperiode: Periode,
        erUtsendelseForOppdragIUtlandet: Boolean?,
        erFortsattAnsattEtterOppdraget: Boolean?,
        erAnsattForOppdragIUtlandet: Boolean?,
        erDrattPaaEgetInitiativ: Boolean?,
        erErstatningTidligereUtsendte: Boolean?
    ) {
        this.samletUtsendingsperiode = samletUtsendingsperiode
        this.erUtsendelseForOppdragIUtlandet = erUtsendelseForOppdragIUtlandet
        this.erFortsattAnsattEtterOppdraget = erFortsattAnsattEtterOppdraget
        this.erAnsattForOppdragIUtlandet = erAnsattForOppdragIUtlandet
        this.erDrattPaaEgetInitiativ = erDrattPaaEgetInitiativ
        this.erErstatningTidligereUtsendte = erErstatningTidligereUtsendte
    }
}
