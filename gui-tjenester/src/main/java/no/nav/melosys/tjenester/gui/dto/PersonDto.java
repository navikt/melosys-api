package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.tjenester.gui.dto.util.DtoUtils.tilLocalDate;

import java.time.LocalDate;

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstand;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap;

public class PersonDto {

    private String fnr;

    private String sivilstand;

    private String statsborgerskap;

    private String kjoenn;

    private String fornavn;

    private String etternavn;

    private String sammensattNavn;

    private LocalDate foedselsdato;

    private BostedsadresseDto bostedsadresse;

    public PersonDto() {
    }

    public static PersonDto tilDto(Person p) {
        PersonDto person = new PersonDto();

        if (p.getAktoer() instanceof PersonIdent) {
            PersonIdent aktoer = (PersonIdent) (p.getAktoer());
            person.setFnr(aktoer.getIdent().getIdent());
        }

        Sivilstand sivilstand = p.getSivilstand();
        // TODO Kodeverk
        person.setSivilstand(sivilstand != null ? sivilstand.getSivilstand().getValue() : null);

        Statsborgerskap statsborgerskap = p.getStatsborgerskap();
        //TODO Kodeverk
        person.setStatsborgerskap(statsborgerskap != null ? statsborgerskap.getLand().getValue() : null);

        Kjoenn kjoenn = p.getKjoenn();
        person.setKjoenn(kjoenn != null ? kjoenn.getKjoenn().getValue() : null);

        Personnavn personnavn = p.getPersonnavn();
        if (personnavn != null) {
            person.setFornavn(personnavn.getFornavn());
            person.setEtternavn(personnavn.getEtternavn());
            person.setSammensattNavn(personnavn.getSammensattNavn());
        }

        Foedselsdato foedselsdato = p.getFoedselsdato();
        if (foedselsdato != null) {
            person.setFoedselsdato(tilLocalDate(foedselsdato.getFoedselsdato()));
        }

        if (p.getBostedsadresse() != null) {
            person.setBostedsadresse(BostedsadresseDto.tilDto(p.getBostedsadresse()));
        }

        return person;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public String getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(String sivilstand) {
        this.sivilstand = sivilstand;
    }

    public String getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(String statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public String getKjoenn() {
        return kjoenn;
    }

    public void setKjoenn(String kjoenn) {
        this.kjoenn = kjoenn;
    }

    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public LocalDate getFoedselsdato() {
        return foedselsdato;
    }
    
    public void setFoedselsdato(LocalDate foedselsdato) {
        this.foedselsdato = foedselsdato;
    }

    public BostedsadresseDto getBostedsadresse() {
        return bostedsadresse;
    }

    public void setBostedsadresse(BostedsadresseDto bostedsadresse) {
        this.bostedsadresse = bostedsadresse;
    }

}