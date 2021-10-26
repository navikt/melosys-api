package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.Master;
import no.nav.melosys.domain.person.Navn;

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

    public no.nav.melosys.domain.person.familie.Familiemedlem tilDomene() {
        final String[] splittetNavn = splitFulltNavn(navn);
        return new no.nav.melosys.domain.person.familie.Familiemedlem(
            new Folkeregisteridentifikator(fnr),
            lagNavn(splittetNavn),
            mapFamilierelasjon(familierelasjon),
            new Foedsel(fødselsdato, null, null, null),
            fnrAnnenForelder == null ? null : new Folkeregisteridentifikator(fnrAnnenForelder),
            null,
            sivilstand == null ? null : lagSivilstand(sivilstand, sivilstandGyldighetsperiodeFom)
        );
    }

    private no.nav.melosys.domain.person.Sivilstand lagSivilstand(Sivilstand sivilstand, LocalDate gyldighetsperiodeFom) {
        return new no.nav.melosys.domain.person.Sivilstand(sivilstand.tilSivilstandstypeFraDomene(), sivilstand.getKode(), "",
                                                           gyldighetsperiodeFom, null, Master.TPS.name(), Master.TPS.name(), false);
    }

    private no.nav.melosys.domain.person.familie.Familierelasjon mapFamilierelasjon(Familierelasjon familierelasjon) {
        return switch (familierelasjon) {
            case BARN -> no.nav.melosys.domain.person.familie.Familierelasjon.BARN;
            case EKTE, REPA, SAM -> no.nav.melosys.domain.person.familie.Familierelasjon.RELATERT_VED_SIVILSTAND;
            case FARA -> no.nav.melosys.domain.person.familie.Familierelasjon.FAR;
            case MORA -> no.nav.melosys.domain.person.familie.Familierelasjon.MOR;
        };
    }

    private Navn lagNavn(String[] splittetNavn) {
        return splittetNavn.length > 2 ? new Navn(splittetNavn[0], splittetNavn[1], splittetNavn[2])
            : new Navn(splittetNavn[0], null, splittetNavn[1]);
    }

    private static String[] splitFulltNavn(String navn) {
        if (navn == null || navn.isEmpty()) {
            return new String[2];
        } else if (!navn.contains(" ")) {
            return new String[]{navn, null};
        } else {
            return navn.split(" ", 3);
        }
    }
}
