package no.nav.melosys.tjenester.gui.dto.periode

import no.nav.melosys.service.kodeverk.KodeDto

class MedlemsperiodeDto {
    @kotlin.jvm.JvmField
    var periodeID: Long = 0

    @kotlin.jvm.JvmField
    var periode: PeriodeDto? = null

    @kotlin.jvm.JvmField
    var periodetype: KodeDto? = null

    @kotlin.jvm.JvmField
    var status: KodeDto? = null

    @kotlin.jvm.JvmField
    var grunnlagstype: KodeDto? = null

    @kotlin.jvm.JvmField
    var land: KodeDto? = null

    @kotlin.jvm.JvmField
    var lovvalg: KodeDto? = null

    @kotlin.jvm.JvmField
    var trygdedekning: KodeDto? = null

    @kotlin.jvm.JvmField
    var kildedokumenttype: KodeDto? = null

    @kotlin.jvm.JvmField
    var kilde: KodeDto? = null
}
