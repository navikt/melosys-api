package no.nav.melosys.service.persondata.mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.stream.Collectors;

import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonMedHistorikk;
import no.nav.melosys.service.persondata.mapping.adresse.BostedsadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.KontaktadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.OppholdsadresseOversetter;

public final class PersonMedHistorikkOversetter {
    private PersonMedHistorikkOversetter() {
        throw new IllegalStateException("Ikke ment å bli instantiert");
    }

    public static PersonMedHistorikk oversett(Person person, KodeverkService kodeverkService) {
        return lagPersonMedHistorikk(person, kodeverkService);
    }

    public static PersonMedHistorikk oversettTilInnsyn(Person person,
                                                       KodeverkService kodeverkService,
                                                       Instant behandlingSistEndretDato) {
        return lagPersonMedHistorikk(gjennskapPersonPå(person, behandlingSistEndretDato), kodeverkService);
    }

    private static Person gjennskapPersonPå(Person person, Instant instant) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return new Person(
            Collections.emptyList(),
            person.bostedsadresse().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.doedsfall().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.foedsel().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.folkeregisteridentifikator().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.folkeregisterpersonstatus().stream().collect(Collectors.toUnmodifiableList()),
            Collections.emptyList(),
            Collections.emptyList(),
            person.kjoenn().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.kontaktadresse().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.navn().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.oppholdsadresse().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.sivilstand().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            person.statsborgerskap().stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableList()),
            Collections.emptyList()
        );
    }

    private static PersonMedHistorikk lagPersonMedHistorikk(Person person, KodeverkService kodeverkService) {
        return new PersonMedHistorikk(
            BostedsadresseOversetter.oversettMedHistorikk(person.bostedsadresse(), kodeverkService),
            DoedsfallOversetter.oversett(person.doedsfall()), FoedselOversetter.oversett(person.foedsel()),
            FolkeregisteridentOversetter.oversett(person.folkeregisteridentifikator()),
            KjoennOversetter.oversett(person.kjoenn()),
            person.kontaktadresse().stream().map(k -> KontaktadresseOversetter.oversett(k, kodeverkService)).collect(
                Collectors.toUnmodifiableSet()), NavnOversetter.oversett(person.navn()),
            person.oppholdsadresse().stream().map(o -> OppholdsadresseOversetter.oversett(o, kodeverkService)).collect(
                Collectors.toUnmodifiableSet()),
            person.statsborgerskap().stream().map(StatsborgerskapOversetter::oversett).collect(
                Collectors.toUnmodifiableSet()));
    }
}
