package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto

class VideresendDto {
    @JvmField
    var mottakerinstitusjon: String? = null
    @JvmField
    var fritekst: String? = null
    @JvmField
    var vedlegg: Collection<VedleggDto> = ArrayList()
}
