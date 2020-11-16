package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Familiemedlem {

    public String fnr;

    public String navn;

    public Familierelasjon familierelasjon;

    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    public boolean borMedBruker;

    public Sivilstand sivilstand;

    public LocalDate sivilstandGyldighetsperiodeFom;

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
