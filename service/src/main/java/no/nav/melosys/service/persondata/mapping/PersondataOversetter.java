package no.nav.melosys.service.persondata.mapping;

import java.util.stream.Collectors;

import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.mapping.adresse.BostedsadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.KontaktadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.OppholdsadresseOversetter;

public final class PersondataOversetter {
    private PersondataOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static Personopplysninger oversett(Person person, KodeverkService kodeverkService) {
        return new Personopplysninger(
            person.adressebeskyttelse().stream().map(AdressebeskyttelseOversetter::oversett).collect(Collectors.toUnmodifiableSet()),
            BostedsadresseOversetter.oversett(person.bostedsadresse(), kodeverkService),
            DoedsfallOversetter.oversett(person.doedsfall()),
            FoedselOversetter.oversett(person.foedsel()),
            FolkeregisteridentOversetter.oversett(person.folkeregisteridentifikator()),
            KjoennOversetter.oversett(person.kjoenn()),
            person.kontaktadresse().stream().map(k -> KontaktadresseOversetter.oversett(k, kodeverkService)).collect(Collectors.toUnmodifiableSet()),
            NavnOversetter.oversett(person.navn()),
            person.oppholdsadresse().stream().map(o -> OppholdsadresseOversetter.oversett(o, kodeverkService)).collect(Collectors.toUnmodifiableSet()),
            person.statsborgerskap().stream().map(StasborgerskapOversetter::oversett).collect(Collectors.toUnmodifiableSet())
        );
    }
}
