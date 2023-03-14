package no.nav.melosys.integrasjon.faktureringskomponenten.dto

data class FakturaserieDto(
    val vedtaksId: String?,
    val fodselsnummer: String?,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,
    val referanseNAV: String?,
    val fakturaGjelder: FakturaGjelder?,
    val intervall: FaktureringsIntervall?,
    val perioder: List<FakturaseriePeriodeDto>?
)
