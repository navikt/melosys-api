package no.nav.melosys.service.dokument.sed.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;
import no.nav.melosys.eux.model.nav.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.dokument.sed.AbstraktSedData;

import static no.nav.melosys.domain.dokument.person.Familierelasjon.FARA;
import static no.nav.melosys.domain.dokument.person.Familierelasjon.MORA;

/**
 * Felles mapper-klasse for alle typer SED. Mapper NAV-objektet i NAV-SED,
 * som brukes av eux for å plukke ut nødvendig informasjon for en angitt SED.
 */
public interface SedMapper<T extends Medlemskap, S extends AbstraktSedData> {

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    //Versjonen til SED'en. Generasjon og versjon (SED_G_VER.SED_VER = 4.1)
    String SED_G_VER = "4";
    String SED_VER = "1";

    //Hvis det skulle trenges noen spesifikke endringer av NAV-objektet for enkelte SED'er,
    // bør denne metoden overrides.
    default SED mapTilSed(S sedData) throws TekniskException, FunksjonellException {
        SED sed = new SED();

        sed.setNav(prefillNav(sedData));
        sed.setSed(getSedType().name());
        sed.setSedGVer(SED_G_VER);
        sed.setSedVer(SED_VER);
        sed.setMedlemskap(hentMedlemskap(sedData));

        return sed;
    }

    T hentMedlemskap(S sedData) throws TekniskException, FunksjonellException;

    SedType getSedType();

    default Nav prefillNav(AbstraktSedData sedData) throws TekniskException {
        Nav nav = new Nav();

        nav.setBruker(hentBruker(sedData.getPersonDokument(), sedData.getSøknadDokument(), sedData.getBostedsadresse()));
        nav.setArbeidssted(hentArbeidssted(sedData.getArbeidssteder()));
        nav.setArbeidsgiver(hentArbeidsGiver(sedData.getArbeidsgivendeVirksomheter()));

        if (sedData.getPersonDokument().erEgenAnsatt && !sedData.getSelvstendigeVirksomheter().isEmpty()) {
            nav.setSelvstendig(hentSelvStendig(sedData.getSelvstendigeVirksomheter()));
        }

        return nav;
    }

    default Bruker hentBruker(PersonDokument personDokument, SoeknadDokument søknadDokument, Bostedsadresse bostedsadresse)
        throws TekniskException {
        Bruker bruker = new Bruker();

        bruker.setPerson(hentPerson(personDokument, søknadDokument));
        bruker.setAdresse(hentAdresser(bostedsadresse));
        setFamiliemedlemmer(personDokument, bruker);

        return bruker;
    }

    default Person hentPerson(PersonDokument personDokument, SoeknadDokument søknadDokument) {
        Person person = new Person();

        person.setFornavn(personDokument.fornavn);
        person.setEtternavn(personDokument.etternavn);
        person.setFoedselsdato(formaterDato(personDokument.fødselsdato));
        person.setFoedested(null); //det antas at ikke trengs når NAV fyller ut.
        person.setKjoenn(personDokument.kjønn.getKode());

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLand(personDokument.statsborgerskap.getKode());

        person.setStatsborgerskap(Collections.singletonList(statsborgerskap));

        person.setPin(hentPin(personDokument, søknadDokument));

        return person;
    }

    default List<Pin> hentPin(PersonDokument personDokument, SoeknadDokument søknadDokument) {
        List<Pin> pins = Lists.newArrayList();

        pins.add(new Pin(
            personDokument.fnr, "NO", null)); //null settes for sektor per nå. Ikke påkrevd. Evt hardkode 'alle'

        søknadDokument.personOpplysninger.utenlandskIdent.stream()
            .map(utenlandskIdent -> new Pin(utenlandskIdent.ident, utenlandskIdent.landKode, null))
            .forEachOrdered(pins::add);

        return pins;
    }

    default List<Adresse> hentAdresser(Bostedsadresse bostedsadresse) {

        Adresse adresse = new Adresse();
        adresse.setBy(bostedsadresse.getPoststed());
        adresse.setPostnummer(bostedsadresse.getPostnr());
        adresse.setLand(bostedsadresse.getLand().toString());
        adresse.setGate(bostedsadresse.getGateadresse().getGatenavn());

        return Collections.singletonList(adresse);
    }

    default void setFamiliemedlemmer(PersonDokument personDokument, Bruker bruker) {

        //Splitter per nå navnet etter første mellomrom
        Optional<Familiemedlem> optionalFar = personDokument.familiemedlemmer.stream()
            .filter(f -> f.familierelasjon.equals(FARA)).findFirst();

        if (optionalFar.isPresent()) {
            Far far = new Far();
            Person person = new Person();
            String[] navn = splitFulltNavn(optionalFar.get().navn);
            person.setEtternavnvedfoedsel(navn[0]);
            person.setFornavn(navn[1]);

            far.setPerson(person);
            bruker.setFar(far);
        }

        Optional<Familiemedlem> optionalMor = personDokument.familiemedlemmer.stream()
            .filter(f -> f.familierelasjon.equals(MORA)).findFirst();

        if (optionalMor.isPresent()) {
            Mor mor = new Mor();
            Person person = new Person();
            String[] navn = splitFulltNavn(optionalMor.get().navn);
            person.setEtternavnvedfoedsel(navn[0]);
            person.setFornavn(navn[1]);

            mor.setPerson(person);
            bruker.setMor(mor);
        }
    }

    default List<Arbeidssted> hentArbeidssted(List<no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted> avklarteArbeidssteder) {

        //Skal bare være èn ved Lev1
        return avklarteArbeidssteder.stream().map(avklartArb -> {
            Arbeidssted arbeidssted = new Arbeidssted();
            arbeidssted.setNavn(avklartArb.navn);
            if (avklartArb.erFysisk()) {
                arbeidssted.setAdresse(hentAdresseFraStrukturertAdresse(avklartArb.adresse));
            } else {
                arbeidssted.setErikkefastadresse("ja");
            }
            return arbeidssted;
        }).collect(Collectors.toList()); //TODO: må tilrettelegge for maritime arbeidssteder når dette blir avklart.ref: BrevByggerBase:hentIkkeFysiskeArbeidssteder()
    }

    default List<Arbeidsgiver> hentArbeidsGiver(List<Virksomhet> arbeidsGivendeVirksomheter) {

        return arbeidsGivendeVirksomheter.stream().map(virksomhet -> {
            Arbeidsgiver arbeidsgiver = new Arbeidsgiver();
            arbeidsgiver.setNavn(virksomhet.navn);
            arbeidsgiver.setAdresse(hentAdresseFraStrukturertAdresse((StrukturertAdresse)virksomhet.adresse));

            Identifikator orgNr = new Identifikator();
            orgNr.setId(virksomhet.orgnr);
            orgNr.setType("registrering"); //organisasjonsindenttypekoder.properties i eux står typer

            arbeidsgiver.setIdentifikator(Collections.singletonList(orgNr));
            return arbeidsgiver;
        }).collect(Collectors.toList());
    }

    default Selvstendig hentSelvStendig(List<Virksomhet> virksomheter) {
        Selvstendig selvstendig = new Selvstendig();

        selvstendig.setArbeidsgiver(
            virksomheter.stream().map(v -> {
                Arbeidsgiver arbeidsgiver = new Arbeidsgiver();

                Identifikator orgNr = new Identifikator();
                orgNr.setId(v.orgnr);
                orgNr.setType("registrering"); //organisasjonsindenttypekoder.properties i eux står typer

                if (!(v.adresse instanceof StrukturertAdresse)) {
                    throw new RuntimeException(
                        "Feil har skjedd ved henting av data. Forventer type: "
                            + StrukturertAdresse.class.getSimpleName() + ", men adresse er av: "
                            + v.adresse.getClass().getSimpleName());
                }

                arbeidsgiver.setIdentifikator(Collections.singletonList(orgNr));
                arbeidsgiver.setAdresse(hentAdresseFraStrukturertAdresse((StrukturertAdresse) v.adresse));
                arbeidsgiver.setNavn(v.navn);

                return arbeidsgiver;
            }).collect(Collectors.toList())
        );

        return selvstendig;
    }

    default String formaterDato(LocalDate dato) {
        return dateTimeFormatter.format(dato);
    }

    default  String[] splitFulltNavn(String navn) {
        if (navn == null || navn.isEmpty()) return new String[2];
        else if (!navn.contains(" ")) return new String[] {navn, null};
        else return navn.split(" ", 2);
    }

    default Adresse hentAdresseFraStrukturertAdresse(StrukturertAdresse sAdresse) {
        Adresse adresse = new Adresse();
        adresse.setGate(sAdresse.gatenavn);
        adresse.setPostnummer(sAdresse.postnummer);
        adresse.setBy(sAdresse.poststed);
        adresse.setRegion(sAdresse.region);
        adresse.setLand(sAdresse.landKode);
        adresse.setBygning(null);

        return adresse;
    }
}
