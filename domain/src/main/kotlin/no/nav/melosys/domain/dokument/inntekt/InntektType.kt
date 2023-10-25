package no.nav.melosys.domain.dokument.inntekt

enum class InntektType {
    // Navn her er for bakoverkompatibilitet med det som allerede er i databasen
    Inntekt,
    Loennsinntekt,
    Naeringsinntekt,
    PensjonEllerTrygd,
    YtelseFraOffentlige,
}
