package no.nav.melosys.integrasjon.faktureringskomponenten.dto

data class FakturaserieDto(
    val vedtaksnummer: String,
    val fodselsnummer: String,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,
    val referanseNAV: String,
    val intervall: FaktureringsIntervall = FaktureringsIntervall.MANEDLIG,
    val perioder: List<FakturaseriePeriodeDto>
)
