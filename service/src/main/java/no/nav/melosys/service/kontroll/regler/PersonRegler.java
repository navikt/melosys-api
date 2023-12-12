package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;

public final class PersonRegler {
    private static final String NORGE_ISO2_LANDKODE = "NO";

    public static boolean erPersonDød(Persondata persondata) {
        return persondata.erPersonDød();
    }

    public static boolean personBosattINorge(Persondata persondata) {
        Optional<Bostedsadresse> bostedsadresseOptional = persondata.finnBostedsadresse();

        return bostedsadresseOptional.isPresent()
            && bostedsadresseOptional.get().strukturertAdresse().getLandkode() != null
            && NORGE_ISO2_LANDKODE.equals(bostedsadresseOptional.get().strukturertAdresse().getLandkode());
    }

    public static boolean personBosattINorgeIPeriode(List<BostedsadressePeriode> bostedsadressePerioder, LocalDate periodeFra, LocalDate periodeTil) {
        return bostedsadressePerioder
            .stream()
            .anyMatch(a ->
                a.bostedsadresse.tilStrukturertAdresse().getLandkode().equals(NORGE_ISO2_LANDKODE) &&
                    ((a.periode.getFom().isBefore(periodeFra) || a.periode.getFom().isEqual(periodeFra)) &&
                        (a.periode.getTom().isAfter(periodeFra) || a.periode.getTom().isEqual(periodeFra))) ||
                    ((a.periode.getFom().isBefore(periodeTil) || a.periode.getFom().isEqual(periodeTil)) &&
                        (a.periode.getTom().isAfter(periodeTil) || a.periode.getTom().isEqual(periodeTil)))
            );

    }

    public static boolean harRegistrertAdresse(Persondata persondata, MottatteOpplysningerData mottatteOpplysningerData) {
        return !persondata.manglerGyldigRegistrertAdresse() || mottatteOpplysningerData.bosted.oppgittAdresse.erGyldig();
    }

    // TODO Bør vi fortsatt ta hensyn til MottatteOpplysningerData her?
    public static boolean harRegistrertAdresse(Persondata persondata) {
        return !persondata.manglerGyldigRegistrertAdresse();
    }
}
