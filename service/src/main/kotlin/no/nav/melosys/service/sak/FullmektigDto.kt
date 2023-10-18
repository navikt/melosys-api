package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Fullmaktstype

data class FullmektigDto(val orgnr: String?, val personident: String?, val fullmakter: List<Fullmaktstype>)
