package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.melosys.domain.dokument.person.adresse.BostedsadressePeriode;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.oppgave.Oppgave;
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

    public static boolean personBosattINorgeIPeriode(List<BostedsadressePeriode> bostedsadressePerioder, Optional<Bostedsadresse> bostedsadresseOptional, LocalDate periodeFra, LocalDate periodeTil) {
        var erGyldigBostedsadresse = bostedsadresseOptional.filter(bostedsadresse -> bostedsadresse.strukturertAdresse().getLandkode() != null &&
            NORGE_ISO2_LANDKODE.equals(bostedsadresse.strukturertAdresse().getLandkode()) &&
            filtrerAdressePeriode(bostedsadresse.gyldigFraOgMed(), bostedsadresse.gyldigTilOgMed(), periodeFra, periodeTil)).isPresent();

        return erGyldigBostedsadresse || bostedsadressePerioder
            .stream()
            .anyMatch(a ->
                a.bostedsadresse.tilStrukturertAdresse().getLandkode().equals(NORGE_ISO2_LANDKODE) &&
                    filtrerAdressePeriode(a.periode.getFom(), a.periode.getTom(), periodeFra, periodeTil)
            );
    }

    private static boolean filtrerAdressePeriode(LocalDate adresseGyldigFom, LocalDate adresseGyldigTom, LocalDate periodeFra, LocalDate periodeTil) {
        var fom = adresseGyldigFom;
        var tom = adresseGyldigTom != null ? adresseGyldigTom : LocalDate.now().plusYears(10);

        if(fom == null){
            return false;
        }

        return ((fom.isBefore(periodeFra) || fom.isEqual(periodeFra)) &&
            (tom.isAfter(periodeFra) || tom.isEqual(periodeFra))) ||
            ((fom.isBefore(periodeTil) || fom.isEqual(periodeTil)) &&
                (tom.isAfter(periodeTil) || tom.isEqual(periodeTil)));
    }

    public static boolean harRegistrertAdresse(Persondata persondata, MottatteOpplysningerData mottatteOpplysningerData) {
        return !persondata.manglerGyldigRegistrertAdresse() || mottatteOpplysningerData.bosted.oppgittAdresse.erGyldig();
    }

    // TODO Bør vi fortsatt ta hensyn til MottatteOpplysningerData her?
    public static boolean harRegistrertAdresse(Persondata persondata) {
        return !persondata.manglerGyldigRegistrertAdresse();
    }
}
