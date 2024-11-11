package no.nav.melosys.integrasjon.faktureringskomponenten.dto

data class FakturaserieDto(
    val fodselsnummer: String,
    val fakturaserieReferanse: String?,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String,
    val referanseNAV: String,
    val fakturaGjelderInnbetalingstype: Innbetalingstype,
    val intervall: FaktureringIntervall,
    val perioder: List<FakturaseriePeriodeDto>
)
