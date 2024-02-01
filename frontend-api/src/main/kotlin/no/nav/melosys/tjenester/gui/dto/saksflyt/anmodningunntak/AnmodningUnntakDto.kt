package no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak

import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto

class AnmodningUnntakDto {
    var mottakerinstitusjon: String? = null
    var fritekstSed: String? = null
    var vedlegg: Collection<VedleggDto> = ArrayList()
}
