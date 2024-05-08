package no.nav.melosys.integrasjon.faktureringskomponenten.dto

data class BeregnTotalBeløpDto(
    val saksbehandlerIdent: String,
    val fakturaseriePerioder: List<FakturaseriePeriodeDto>
)
