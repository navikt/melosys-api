package no.nav.melosys.tjenester.gui.dto.periode

import no.nav.melosys.service.kodeverk.KodeDto

class MedlemsperiodeDto {

    var periodeID: Long = 0L

    var periode: PeriodeDto? = null

    var periodetype: KodeDto? = null

    var status: KodeDto? = null

    var grunnlagstype: KodeDto? = null

    var land: KodeDto? = null

    var lovvalg: KodeDto? = null

    var trygdedekning: KodeDto? = null

    var kildedokumenttype: KodeDto? = null

    var kilde: KodeDto? = null
}
