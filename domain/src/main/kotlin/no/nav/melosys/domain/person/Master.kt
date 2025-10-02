package no.nav.melosys.domain.person

// Hvem er master for personopplysninger (https://navikt.github.io/pdl/#mastring)
enum class Master {
    FREG,
    PDL,
    TPS
}