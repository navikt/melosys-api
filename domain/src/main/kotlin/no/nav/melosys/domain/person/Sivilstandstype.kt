package no.nav.melosys.domain.person

enum class Sivilstandstype {
    ENKE_ELLER_ENKEMANN,
    GIFT,
    GJENLEVENDE_PARTNER,
    REGISTRERT_PARTNER,
    SEPARERT,
    SEPARERT_PARTNER,
    SKILT,
    SKILT_PARTNER,
    UDEFINERT,
    UGIFT,
    UOPPGITT;

    fun erUdefinert(): Boolean = this == UDEFINERT

    override fun toString(): String = when (this) {
        ENKE_ELLER_ENKEMANN -> "Enke eller enkemann"
        GIFT -> "Gift"
        GJENLEVENDE_PARTNER -> "Gjenlevende partner"
        REGISTRERT_PARTNER -> "Registrert partner"
        SEPARERT -> "Separert"
        SEPARERT_PARTNER -> "Separert partner"
        SKILT -> "Skilt"
        SKILT_PARTNER -> "Skilt partner"
        UDEFINERT -> "Udefinert"
        UGIFT -> "Ugift"
        UOPPGITT -> "Uoppgitt"
    }
}