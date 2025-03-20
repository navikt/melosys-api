package no.nav.melosys.service.persondata.mapping;

import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Familierelasjonsrolle;
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;

public final class FamiliemedlemOversetter {
    private FamiliemedlemOversetter() {
    }

    public static Familiemedlem oversettBarn(Person barn,
                                             Folkeregisteridentifikator folkeregisteridentGjeldendeForelder) {
        return new Familiemedlem(
                FolkeregisteridentOversetter.oversett(barn.folkeregisteridentifikator()),
                NavnOversetter.oversett(barn.navn()),
                Familierelasjon.BARN,
                FoedselOversetter.oversett(barn.foedested(), barn.foedselsdato()),
                hentFnrAnnenForelder(barn, folkeregisteridentGjeldendeForelder),
                ForeldreansvarOversetter.oversett(barn.foreldreansvar()),
                null
        );
    }

    private static Folkeregisteridentifikator hentFnrAnnenForelder(Person barn,
                                                                   Folkeregisteridentifikator folkeregisterident) {
        final String fnrGjeldendeForelder = folkeregisterident.identifikasjonsnummer();
        return barn.forelderBarnRelasjon().stream()
                .filter(ForelderBarnRelasjon::erForelder)
                .filter(forelderBarnRelasjon -> !fnrGjeldendeForelder.equals(forelderBarnRelasjon.relatertPersonsIdent()))
                .findAny()
                .map(forelderBarnRelasjon -> new Folkeregisteridentifikator(forelderBarnRelasjon.relatertPersonsIdent()))
                .orElse(null);
    }

    public static Familiemedlem oversettForelder(Person forelder, Familierelasjonsrolle familierelasjonsrolle) {
        return new Familiemedlem(
                FolkeregisteridentOversetter.oversett(forelder.folkeregisteridentifikator()),
                NavnOversetter.oversett(forelder.navn()),
                oversettTilFamilierelasjonForeldre(familierelasjonsrolle),
                FoedselOversetter.oversett(forelder.foedested(), forelder.foedselsdato()),
                null,
                null,
                null
        );
    }

    private static Familierelasjon oversettTilFamilierelasjonForeldre(Familierelasjonsrolle familierelasjonsrolle) {
        return switch (familierelasjonsrolle) {
            case FAR -> Familierelasjon.FAR;
            case MOR -> Familierelasjon.MOR;
            default -> throw new IllegalStateException("Unexpected value: " + familierelasjonsrolle);
        };
    }

    public static Familiemedlem oversettEktefelleEllerPartner(Person person, no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand sivilstand) {
        return new Familiemedlem(
                FolkeregisteridentOversetter.oversett(person.folkeregisteridentifikator()),
                NavnOversetter.oversett(person.navn()),
                Familierelasjon.RELATERT_VED_SIVILSTAND,
                FoedselOversetter.oversett(person.foedested(), person.foedselsdato()),
                null,
                null,
                SivilstandOversetter.oversett(sivilstand)
        );
    }
}
