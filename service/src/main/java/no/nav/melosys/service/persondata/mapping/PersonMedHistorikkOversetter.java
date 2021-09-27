package no.nav.melosys.service.persondata.mapping;

import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonMedHistorikk;
import no.nav.melosys.service.persondata.mapping.adresse.BostedsadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.KontaktadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.OppholdsadresseOversetter;

public final class PersonMedHistorikkOversetter {
    private PersonMedHistorikkOversetter() {
    }

    public static PersonMedHistorikk oversett(Person person, KodeverkService kodeverkService) {
        return lagPersonMedHistorikk(person, kodeverkService);
    }

    private static PersonMedHistorikk lagPersonMedHistorikk(Person person, KodeverkService kodeverkService) {
        return new PersonMedHistorikk(
            person.bostedsadresse().stream()
                .map(a -> BostedsadresseOversetter.oversett(a, kodeverkService))
                .flatMap(Optional::stream).collect(Collectors.toUnmodifiableSet()),
            DoedsfallOversetter.oversett(person.doedsfall()),
            FoedselOversetter.oversett(person.foedsel()),
            FolkeregisteridentOversetter.oversett(person.folkeregisteridentifikator()),
            FolkeregisterpersonstatusOversetter.oversett(person.folkeregisterpersonstatus()),
            KjoennOversetter.oversett(person.kjoenn()),
            person.kontaktadresse().stream().map(k -> KontaktadresseOversetter.oversett(k, kodeverkService))
                .collect(Collectors.toUnmodifiableSet()),
            NavnOversetter.oversett(person.navn()),
            person.oppholdsadresse().stream().map(o -> OppholdsadresseOversetter.oversett(o, kodeverkService))
                .collect(Collectors.toUnmodifiableSet()),
            person.sivilstand().stream().map(SivilstandOversetter::oversett).collect(Collectors.toUnmodifiableSet()),
            person.statsborgerskap().stream().map(StatsborgerskapOversetter::oversett)
                .collect(Collectors.toUnmodifiableSet()));
    }
}
