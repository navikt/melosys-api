package no.nav.melosys.service.persondata.mapping;

import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.familie.Familierelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Familierelasjonsrolle;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;

public final class FamiliemedlemOversetter {
    private FamiliemedlemOversetter() {
    }

    public static Familiemedlem oversettBarn(Person barn) {
        return new Familiemedlem(
            FolkeregisteridentOversetter.oversett(barn.folkeregisteridentifikator()),
            NavnOversetter.oversett(barn.navn()),
            Familierelasjon.BARN,
            FoedselOversetter.oversett(barn.foedsel())
        );
    }

    public static Familiemedlem oversettForelder(Person forelder, Familierelasjonsrolle familierelasjonsrolle) {
        return new Familiemedlem(
            FolkeregisteridentOversetter.oversett(forelder.folkeregisteridentifikator()),
            NavnOversetter.oversett(forelder.navn()),
            oversettTilFamilierelasjonForeldre(familierelasjonsrolle),
            FoedselOversetter.oversett(forelder.foedsel())
        );
    }

    private static Familierelasjon oversettTilFamilierelasjonForeldre(Familierelasjonsrolle familierelasjonsrolle) {
        return switch (familierelasjonsrolle) {
            case FAR -> Familierelasjon.FAR;
            case MOR -> Familierelasjon.MOR;
            default -> throw new IllegalStateException("Unexpected value: " + familierelasjonsrolle);
        };
    }

    public static Familiemedlem oversettRelatertVedSivilstand(Person relatertVedSivilstand) {
        return new Familiemedlem(
            FolkeregisteridentOversetter.oversett(relatertVedSivilstand.folkeregisteridentifikator()),
            NavnOversetter.oversett(relatertVedSivilstand.navn()),
            Familierelasjon.RELATERT_VED_SIVILSTAND,
            FoedselOversetter.oversett(relatertVedSivilstand.foedsel())
        );
    }
}
