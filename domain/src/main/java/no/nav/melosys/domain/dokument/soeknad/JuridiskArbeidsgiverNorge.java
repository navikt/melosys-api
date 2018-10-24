package no.nav.melosys.domain.dokument.soeknad;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Opplysninger om juridiske arbeidsgivere i Norge
 * Opplysningene er for å kunne vurdere vesentlig virksomhet i Norge.
 * De er bare relevant når det gjelder utsendt arbeidstaker og pre-utfyllingen fra informasjon innsendt tidligere (fra samme arbeidsgiver) er eldre enn 12 måneder.
 */
public class JuridiskArbeidsgiverNorge {
    public boolean erBemanningsbyra;
    public int utsendteNeste12Mnd;
    public int antallAdmAnsatte;
    @JsonProperty("antallAdminAnsatteEOS")
    public int antallAdminAnsatteEØS;
    public BigDecimal andelOmsetningINorge;
    public BigDecimal andelKontrakterINorge;
    public boolean utsendtFortsetterArbeidsforholdIUtlandet;
}
