package no.nav.melosys.tjenester.gui.dto.eessi

import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto

@JvmRecord
data class BucBestillingDto(
    @JvmField val bucType: BucType,
    @JvmField val mottakerInstitusjoner: List<String>,
    @JvmField val vedlegg: Collection<VedleggDto>
)
