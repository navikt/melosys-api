package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;

public class PersonDto {

    public PersonDto(PersonDokument person) {
        fnr = person.fnr;
        sivilstand = person.sivilstand;
        statsborgerskap = person.statsborgerskap;
        sammensattNavn = person.sammensattNavn;
        personstatus = person.personstatus;
        kjønn = person.kjønn;
        fødselsdato = person.fødselsdato;
        bostedsadresse = person.bostedsadresse;
    }

    public String fnr;

    public Sivilstand sivilstand;

    public Land statsborgerskap;

    public String sammensattNavn;

    public Personstatus personstatus;

    @JsonProperty("kjoenn")
    public KjoennsType kjønn;

    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    public Bostedsadresse bostedsadresse;
}
