package no.nav.melosys.service.persondata.mapping;

import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.mapping.adresse.BostedsadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.KontaktadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.OppholdsadresseOversetter;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class PersonopplysningerOversetter {
    private PersonopplysningerOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Personopplysninger oversett(Person person, KodeverkService kodeverkService) {
        return oversettMedFamilie(person, Collections.emptySet(), kodeverkService);
    }

    public static Personopplysninger oversettMedFamilie(Person person, Set<Familiemedlem> familiemedlemmer,
                                                        KodeverkService kodeverkService) {
        return new Personopplysninger(
            person.adressebeskyttelse().stream().map(AdressebeskyttelseOversetter::oversett).collect(Collectors.toUnmodifiableSet()),
            BostedsadresseOversetter.finnGjeldende(person.bostedsadresse(), kodeverkService),
            DoedsfallOversetter.oversett(person.doedsfall()),
            familiemedlemmer,
            FoedselOversetter.oversett(person.foedsel()),
            FolkeregisteridentOversetter.oversett(person.folkeregisteridentifikator()),
            KjoennOversetter.oversett(person.kjoenn()),
            person.kontaktadresse().stream().map(k -> KontaktadresseOversetter.oversett(k, kodeverkService)).collect(Collectors.toUnmodifiableSet()),
            NavnOversetter.oversett(person.navn()),
            person.oppholdsadresse().stream().map(o -> OppholdsadresseOversetter.oversett(o, kodeverkService)).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet()),
            person.statsborgerskap().stream().map(StatsborgerskapOversetter::oversett).collect(Collectors.toUnmodifiableSet())
        );
    }
}
