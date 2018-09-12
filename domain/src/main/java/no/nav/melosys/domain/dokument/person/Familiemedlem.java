package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Familiemedlem {

    public String fnr;

    @JsonProperty("sammensattNavn")
    public String navn;

    @JsonProperty("relasjonstype")
    public Familierelasjon familierelasjon;
}
