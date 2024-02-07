package no.nav.melosys.domain.dokument.inntekt

data class ArbeidsforholdFrilanser(
    val frilansPeriode: Periode? = null,
    val yrke: String? = null //"http://nav.no/kodeverk/Kodeverk/Yrker"
)
