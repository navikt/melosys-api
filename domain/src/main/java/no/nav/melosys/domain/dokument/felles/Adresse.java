package no.nav.melosys.domain.dokument.felles;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Adresse {
    public String landkode;

    @JsonIgnore
    public abstract boolean erTom();
}
