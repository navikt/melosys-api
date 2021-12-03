package no.nav.melosys.service.persondata.mapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
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

    public static PersonMedHistorikk lagHistorikkFraTpsData(PersonDokument personDokument,
                                                            KodeverkService kodeverkService) {
        return new PersonMedHistorikk(
            personDokument.finnBostedsadresse().map(Collections::singleton).orElseGet(Collections::emptySet),
            new Doedsfall(personDokument.getDødsdato()),
            new Foedsel(personDokument.getFødselsdato(), personDokument.getFødselsdato().getYear(), null, null),
            new Folkeregisteridentifikator(personDokument.getFnr()),
            new Folkeregisterpersonstatus(Personstatuser.UDEFINERT, kodeverkService.dekod(FellesKodeverk.PERSONSTATUSER, personDokument.getPersonstatus().getKode())),
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
        return new Sivilstand(sivilstandstype, tekstHvisTypeErUdefinert, "", gyldighetsperiodeFom, null, Master.TPS.name(),
                              Master.TPS.name(), false);
    }

    private static Statsborgerskap lagStatsborgerskap(Land statsborgerskap, LocalDate statsborgerskapDato) {
        return new Statsborgerskap(statsborgerskap.getKode(), null, statsborgerskapDato, null, Master.TPS.name(), Master.TPS.name(), false);
    }
}
