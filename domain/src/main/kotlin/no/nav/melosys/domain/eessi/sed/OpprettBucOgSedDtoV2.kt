package no.nav.melosys.domain.eessi.sed

import no.nav.melosys.domain.eessi.BucType

data class OpprettBucOgSedDtoV2(
    val bucType: BucType,
    val sedDataDto: SedDataDto,
    val vedlegg: Collection<VedleggReferanse> = emptySet(),
    val sendAutomatisk: Boolean,
    val oppdaterEksisterende: Boolean = false
)
