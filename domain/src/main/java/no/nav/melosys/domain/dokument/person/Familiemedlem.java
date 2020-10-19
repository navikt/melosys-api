package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Familiemedlem {

    public String fnr;

    @JsonProperty("sammensattNavn")
    public String navn;

    @JsonProperty("relasjonstype")
    public Familierelasjon familierelasjon;

    @JsonIgnore
    public LocalDate fødselsdato;

    @JsonIgnore
    public boolean borMedBruker;

    @JsonIgnore
    public Sivilstand sivilstand;

    @JsonIgnore
    public LocalDate sivilstandGyldighetsperiodeFom;

    @JsonIgnore
    public String fnrAnnenForelder;

    public boolean erBarn() {
        return familierelasjon == Familierelasjon.BARN;
    }

    public boolean erForelder() {
        return familierelasjon == Familierelasjon.FARA
            || familierelasjon == Familierelasjon.MORA;
    }

    public boolean erEktefellePartnerSamboer() {
        return familierelasjon == Familierelasjon.EKTE
            || familierelasjon == Familierelasjon.REPA
            || familierelasjon == Familierelasjon.SAM;
    }
}
