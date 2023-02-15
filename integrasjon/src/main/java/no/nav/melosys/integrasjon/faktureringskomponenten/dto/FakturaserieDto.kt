package no.nav.melosys.integrasjon.faktureringskomponenten.dto

data class FakturaserieDto(
    val vedtaksId: String?,
    val fodselsnummer: String?,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,
    val referanseNAV: String?,
    val fakturaGjelder: String?,
    val intervall: FaktureringsIntervall? = FaktureringsIntervall.MANEDLIG,
    val perioder: List<FakturaseriePeriodeDto>?
)
