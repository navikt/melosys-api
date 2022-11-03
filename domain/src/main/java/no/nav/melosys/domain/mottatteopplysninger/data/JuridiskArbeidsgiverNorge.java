package no.nav.melosys.domain.mottatteopplysninger.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Opplysninger om juridiske arbeidsgivere i Norge
 * Opplysningene er for å kunne vurdere vesentlig virksomhet i Norge.
 * De er bare relevant når det gjelder utsendt arbeidstaker og pre-utfyllingen fra informasjon innsendt tidligere (fra samme arbeidsgiver) er eldre enn 12 måneder.
 */
public class JuridiskArbeidsgiverNorge {
    public Integer antallAdmAnsatte;
    public Integer antallAnsatte;
    public Integer antallUtsendte;
    public BigDecimal andelOmsetningINorge;
    public BigDecimal andelOppdragINorge;
    public BigDecimal andelKontrakterINorge;
    public BigDecimal andelRekruttertINorge;
    public List<String> ekstraArbeidsgivere = new ArrayList<>();
    public Boolean erOffentligVirksomhet;

    public Stream<String> hentManueltRegistrerteArbeidsgiverOrgnumre() {
        return ekstraArbeidsgivere.stream();
    }
}
