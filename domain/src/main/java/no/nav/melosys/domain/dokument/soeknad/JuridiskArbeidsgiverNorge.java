package no.nav.melosys.domain.dokument.soeknad;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Opplysninger om juridiske arbeidsgivere i Norge
 * Opplysningene er for å kunne vurdere vesentlig virksomhet i Norge.
 * De er bare relevant når det gjelder utsendt arbeidstaker og pre-utfyllingen fra informasjon innsendt tidligere (fra samme arbeidsgiver) er eldre enn 12 måneder.
 */
public class JuridiskArbeidsgiverNorge {
    public Integer antallAnsatte;
    public Integer antallAdminAnsatte;
    @JsonProperty("antallAdminAnsatteEOS")
    public Integer antallAdminAnsatteEØS;
    public BigDecimal andelOmsetningINorge;
    public BigDecimal andelKontrakterINorge;
    public Boolean erBemanningsbyra;
    public Boolean hattDriftSiste12Mnd;
    public Integer antallUtsendte;
}
