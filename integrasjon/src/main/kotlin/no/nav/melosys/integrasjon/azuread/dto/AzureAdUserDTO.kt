package no.nav.melosys.integrasjon.azuread.dto

// Graph returnerer null for givenName/surname på enkelte kontotyper.
data class AzureAdUserDTO(val givenName: String?, val surname: String?)
