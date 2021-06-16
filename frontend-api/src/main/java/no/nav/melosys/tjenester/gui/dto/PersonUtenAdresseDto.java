package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.person.Persondata;

public class PersonUtenAdresseDto {

    private String fnr;
    private Land statsborgerskap;
    private LocalDate statsborgerskapDato;
    private Sivilstand sivilstand;
    private String sammensattNavn;
    private List<FamiliemedlemDto> familiemedlemmer = new ArrayList<>();
    private Personstatus personStatus;
    private KjoennDto kjoenn;
    private LocalDate foedselsdato;
    @JsonProperty(defaultValue = "false" )
    private boolean erEgenAnsatt; // MELOSYS-1580

    public PersonUtenAdresseDto() {}

    public PersonUtenAdresseDto(Persondata person) {
        fnr = person.hentFolkeregisterIdent();
        sivilstand = person.getSivilstand();
        statsborgerskap = person.hentAlleStatsborgerskap().stream().findFirst().orElse(null);
        statsborgerskapDato = person.getStatsborgerskapDato();
        sammensattNavn = person.getSammensattNavn();
        if (person.getFamiliemedlemmer() != null) {
            familiemedlemmer = FamiliemedlemDto.avFamiliemedlemmer(person.getFamiliemedlemmer());
        }
        personStatus = person.getPersonstatus();
        kjoenn = new KjoennDto(person.hentKjønnType().getKode(), person.hentKjønnType().toString());;
        foedselsdato = person.getFødselsdato();
    }

    public String getFnr() {
        return fnr;
    }

    public Land getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(Land statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public LocalDate getStatsborgerskapDato() {
        return statsborgerskapDato;
    }

    public void setStatsborgerskapDato(LocalDate statsborgerskapDato) {
        this.statsborgerskapDato = statsborgerskapDato;
    }

    public Sivilstand getSivilstand() {
        return sivilstand;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public List<FamiliemedlemDto> getFamiliemedlemmer() {
        return familiemedlemmer;
    }

    public Personstatus getPersonStatus() {
        return personStatus;
    }

    public KjoennDto getKjoenn() {
        return kjoenn;
    }

    public LocalDate getFoedselsdato() {
        return foedselsdato;
    }

    public boolean isErEgenAnsatt() {
        return erEgenAnsatt;
    }
}
