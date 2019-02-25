package no.nav.melosys.domain.dokument.soeknad;

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
    public int utsendteNeste12Mnd;
    public int antallAdmAnsatte;
    public int antallAnsatte;
    public BigDecimal andelOmsetningINorge;
    public BigDecimal andelOppdragINorge;
    public BigDecimal andelKontrakterINorge;
    public String arbeidstakereRekruttert;
    public String oppdragsKontrakterIHovedsakInngaattILand;
    public List<String> ekstraArbeidsgivere = new ArrayList<>();

    public Stream<String> hentAlleOrganisasjonsnumre() {
        return ekstraArbeidsgivere.stream();
    }
}