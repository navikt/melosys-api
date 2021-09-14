package no.nav.melosys.domain.person;

public enum Sivilstandstype {
    ENKE_ELLER_ENKEMANN,
    GIFT,
    GJENLEVENDE_PARTNER,
    REGISTRERT_PARTNER,
    SEPARERT,
    SEPARERT_PARTNER,
    SKILT,
    SKILT_PARTNER,
    UGIFT,
    UOPPGITT;

    @Override
    public String toString() {
        return switch (this) {
            case ENKE_ELLER_ENKEMANN -> "Enke eller enkemann";
            case GIFT -> "Gift";
            case GJENLEVENDE_PARTNER -> "Gjenlevende partner";
            case REGISTRERT_PARTNER -> "Registrert partner";
            case SEPARERT -> "Separert";
            case SEPARERT_PARTNER -> "Separert partner";
            case SKILT -> "Skilt";
            case SKILT_PARTNER -> "Skilt partner";
            case UGIFT -> "Ugift";
            case UOPPGITT -> "Uoppgitt";
        };
    }
}
