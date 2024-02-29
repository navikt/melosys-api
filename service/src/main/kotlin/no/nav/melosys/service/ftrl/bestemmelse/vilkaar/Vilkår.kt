package no.nav.melosys.service.ftrl.bestemmelse.vilkaar

import no.nav.melosys.domain.kodeverk.Vilkaar

data class Vilkår(
    val vilkår: Vilkaar,
    val muligeBegrunnelser: Collection<String> = emptyList(),
    val defaultOppfylt: Boolean? = null
)
