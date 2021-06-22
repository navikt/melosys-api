package no.nav.melosys.domain.person;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.person.adresse.Adressebeskyttelse;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.exception.IkkeFunnetException;

import static no.nav.melosys.domain.person.Master.FREG;
import static no.nav.melosys.domain.person.Master.PDL;

public record Personopplysninger(
    Collection<Adressebeskyttelse> adressebeskyttelser,
    Bostedsadresse bostedsadresse,
    Doedsfall dødsfall,
    Foedsel fødsel,
    Folkeregisteridentifikator folkeregisteridentifikator,
    KjoennType kjønn,
    Collection<Kontaktadresse> kontaktadresser,
    Navn navn,
    Collection<Oppholdsadresse> oppholdsadresser,
    Collection<Statsborgerskap> statsborgerskap
) implements Persondata {
    @Override
    public boolean erPersonDød() {
        return dødsfall != null && dødsfall.dødsdato() != null;
    }

    @Override
    public Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public boolean harStrengtAdressebeskyttelse() {
        return adressebeskyttelser().stream().anyMatch(Adressebeskyttelse::erStrengtFortrolig);
    }

    @Override
    public boolean harIkkeRegistrertAdresse() {
        return bostedsadresse == null && kontaktadresser.isEmpty() && oppholdsadresser.isEmpty();
    }

    @Override
    public boolean manglerBostedsadresse() {
        return hentBostedsadresse().isEmpty();
    }

    @Override
    public String hentFolkeregisterIdent() {
        return (folkeregisteridentifikator == null) ? null : folkeregisteridentifikator.identifikasjonsnummer();
    }

    @Override
    public Set<Land> hentAlleStatsborgerskap() {
        return statsborgerskap().stream().map(s -> Land.av(s.landkode())).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public KjoennType hentKjønnType() {
        return kjønn;
    }

    @Override
    public String getFornavn() {
        return navn.fornavn();
    }

    @Override
    public String getMellomnavn() {
        return navn.mellomnavn();
    }

    @Override
    public String getEtternavn() {
        return navn.etternavn();
    }

    @Override
    public String getSammensattNavn() {
        return navn.tilSammensattNavn();
    }

    @Override
    public List<Familiemedlem> getFamiliemedlemmer() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public LocalDate getFødselsdato() {
        return fødsel.fødselsdato();
    }

    @Override
    public no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse getBostedsadresse() {
        throw new UnsupportedOperationException("Eksisterer bare ifm. TPS");
    }

    @Override
    public Optional<Bostedsadresse> hentBostedsadresse() {
        return Optional.ofNullable(bostedsadresse);
    }

    @Override
    /*
     * Vi følger anbefaling fra PDL om følgende prioritering:
     * Kontaktadresse med master PDL
     * Kontaktadresse fra Freg med nyeste registreringsdato
     * Oppholdsadresse med master PDL
     * Oppholdsadresse med master Freg
     * Bostedsadresse
     */
    public Postadresse hentGjeldendePostadresse() {
        return lagPostadresseFraKontaktadresser()
            .or(this::lagPostadresseFraOppholdsadresser)
            .orElse(lagPostadresseFraBostedsadresse());
    }

    private Optional<Postadresse> lagPostadresseFraKontaktadresser() {
        return hentGjeldendeKontaktadresseFraMaster(PDL)
            .or(() -> hentGjeldendeKontaktadresseFraMaster(FREG))
            .map(Kontaktadresse::tilPostadresse);
    }

    private Optional<Kontaktadresse> hentGjeldendeKontaktadresseFraMaster(Master master) {
        return kontaktadresser.stream()
            .filter(a -> master.name().equals(a.master()))
            .max(Comparator.comparing(Kontaktadresse::registrertDato));
    }

    private Optional<Postadresse> lagPostadresseFraOppholdsadresser() {
        return hentGjeldendeOppholdsadresseFraMaster(PDL)
            .or(() -> hentGjeldendeOppholdsadresseFraMaster(FREG))
            .map(Oppholdsadresse::strukturertAdresse)
            .map(Postadresse::lagPostadresse);
    }

    private Optional<Oppholdsadresse> hentGjeldendeOppholdsadresseFraMaster(Master master) {
        return oppholdsadresser.stream()
            .filter(a -> master.name().equals(a.master()))
            .max(Comparator.comparing(Oppholdsadresse::registrertDato));
    }

    private Postadresse lagPostadresseFraBostedsadresse() {
        return hentBostedsadresse()
            .map(Bostedsadresse::strukturertAdresse)
            .map(Postadresse::lagPostadresse)
            .orElseThrow(() -> new IkkeFunnetException("Forventer bostedsadresse"));
    }
}
