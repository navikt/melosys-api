package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.Personstatus;
import no.nav.melosys.domain.kodeverk.Personstatuser;
import no.nav.melosys.domain.person.*;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.mapping.adresse.BostedsadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.KontaktadresseOversetter;
import no.nav.melosys.service.persondata.mapping.adresse.OppholdsadresseOversetter;

public final class PersonMedHistorikkOversetter {
    private PersonMedHistorikkOversetter() {
    }

    public static PersonMedHistorikk oversett(Person person, KodeverkService kodeverkService) {
        return new PersonMedHistorikk(
            person.bostedsadresse().stream()
                .map(a -> BostedsadresseOversetter.oversett(a, kodeverkService))
                .flatMap(Optional::stream).collect(Collectors.toUnmodifiableSet()),
            DoedsfallOversetter.oversett(person.doedsfall()),
            FoedselsdatoOversetter.oversett(person.foedselsdato()),
            FolkeregisteridentOversetter.oversett(person.folkeregisteridentifikator()),
            person.folkeregisterpersonstatus().stream()
                .map(status -> FolkeregisterpersonstatusOversetter.oversett(status))
                .filter(status -> status != null)
                .collect(Collectors.toUnmodifiableSet()),
            KjoennOversetter.oversett(person.kjoenn()),
            person.kontaktadresse().stream().map(k -> KontaktadresseOversetter.oversett(k, kodeverkService))
                .collect(Collectors.toUnmodifiableSet()),
            NavnOversetter.oversett(person.navn()),
            person.oppholdsadresse().stream().map(o -> OppholdsadresseOversetter.oversett(o, kodeverkService))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()),
            person.sivilstand().stream().map(SivilstandOversetter::oversett).collect(Collectors.toUnmodifiableSet()),
            person.statsborgerskap().stream().map(StatsborgerskapOversetter::oversett)
                .collect(Collectors.toUnmodifiableSet()));
    }

    public static PersonMedHistorikk lagHistorikkFraTpsData(PersonDokument personDokument,
                                                            KodeverkService kodeverkService) {
        return new PersonMedHistorikk(
            personDokument.finnBostedsadresse().map(Collections::singleton).orElseGet(Collections::emptySet),
            new Doedsfall(personDokument.getDødsdato()),
            new Foedselsdato(personDokument.getFødselsdatoDato(), personDokument.getFødselsdatoDato().getYear()),
            new Folkeregisteridentifikator(personDokument.getFnr()),
            Collections.singleton(lagFolkeregisterpersonstatus(personDokument.getPersonstatus(), kodeverkService)),
            personDokument.hentKjønnType(),
            personDokument.finnKontaktadresse().map(Collections::singleton).orElseGet(Collections::emptySet),
            new Navn(personDokument.getFornavn(), personDokument.getMellomnavn(), personDokument.getEtternavn()),
            personDokument.finnOppholdsadresse().map(Collections::singleton).orElseGet(Collections::emptySet),
            sivilstandFraTps(personDokument.getSivilstand(), personDokument.getSivilstandGyldighetsperiodeFom(), kodeverkService),
            Collections.singleton(lagStatsborgerskap(personDokument.getStatsborgerskap(), personDokument.getStatsborgerskapDato())));
    }

    private static Set<Sivilstand> sivilstandFraTps(no.nav.melosys.domain.dokument.person.Sivilstand sivilstand,
                                                    LocalDate gyldighetsperiodeFom,
                                                    KodeverkService kodeverkService) {
        if (sivilstand == null) {
            return Collections.emptySet();
        }

        Sivilstandstype sivilstandstype = sivilstand.tilSivilstandstypeFraDomene();
        String tekstHvisTypeErUdefinert = "";
        if (sivilstandstype.erUdefinert() && sivilstand.getKode() != null) {
            tekstHvisTypeErUdefinert = kodeverkService.dekod(FellesKodeverk.SIVILSTANDER, sivilstand.getKode());
        }
        return Collections.singleton(lagSivilstand(sivilstandstype, tekstHvisTypeErUdefinert, gyldighetsperiodeFom));
    }

    private static Sivilstand lagSivilstand(Sivilstandstype sivilstandstype,
                                            String tekstHvisTypeErUdefinert,
                                            LocalDate gyldighetsperiodeFom) {
        return new Sivilstand(sivilstandstype,
            tekstHvisTypeErUdefinert,
            "",
            gyldighetsperiodeFom,
            null,
            Master.TPS.name(),
            Master.TPS.name(),
            false);
    }

    private static Folkeregisterpersonstatus lagFolkeregisterpersonstatus(Personstatus personstatus, KodeverkService kodeverkservice) {
        return new Folkeregisterpersonstatus(
            kodeToPersonstatuser(personstatus.getKode()),
            kodeverkservice.dekod(FellesKodeverk.PERSONSTATUSER, personstatus.getKode()),
            Master.TPS.name(),
            Master.TPS.name(),
            null,
            false);
    }

    private static Statsborgerskap lagStatsborgerskap(Land statsborgerskap, LocalDate statsborgerskapDato) {
        return new Statsborgerskap(statsborgerskap.getKode(), null, statsborgerskapDato, null, Master.TPS.name(), Master.TPS.name(), false);
    }

    private static Personstatuser kodeToPersonstatuser(String kode) {
        return switch (kode) {
            case "bosatt" -> Personstatuser.BOSATT;
            case "utflyttet" -> Personstatuser.UTFLYTTET;
            case "forsvunnet" -> Personstatuser.FORSVUNNET;
            case "doed" -> Personstatuser.DOED;
            case "opphoert" -> Personstatuser.OPPHOERT;
            case "foedselsregistrert" -> Personstatuser.FOEDSELSREGISTRERT;
            case "ikkeBosatt" -> Personstatuser.IKKE_BOSATT;
            case "midlertidig" -> Personstatuser.MIDLERTIDIG;
            case "inaktiv" -> Personstatuser.INAKTIV;
            default -> Personstatuser.UDEFINERT;
        };
    }
}
