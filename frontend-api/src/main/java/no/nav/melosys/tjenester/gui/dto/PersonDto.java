package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.Persondata;

public class PersonDto {

    public PersonDto(Persondata person) {
        fnr = person.hentFolkeregisterIdent();
        sivilstand = person.getSivilstand();
        statsborgerskap = person.hentAlleStatsborgerskap().stream().findFirst().orElse(null);
        sammensattNavn = person.getSammensattNavn();
        personstatus = person.getPersonstatus();
        kjoenn = new KjoennDto(person.hentKjønnType().getKode(), person.hentKjønnType().toString());
        foedselsdato = person.getFødselsdato();
        bostedsadresse = person.getBostedsadresse();
        familiemedlemmer = person.getFamiliemedlemmer() == null ? new ArrayList<>()
            : FamiliemedlemDto.avFamiliemedlemmer(person.getFamiliemedlemmer());
    }

    public String fnr;

    public Land statsborgerskap;

    public Sivilstand sivilstand;

    public String sammensattNavn;

    public Personstatus personstatus;

    public KjoennDto kjoenn;

    public LocalDate foedselsdato;

    public Bostedsadresse bostedsadresse;

    public List<FamiliemedlemDto> familiemedlemmer;
}
