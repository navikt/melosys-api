package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;

public class Familiemedlem {

    public String fnr;

    public String navn;

    public Familierelasjon familierelasjon;

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
