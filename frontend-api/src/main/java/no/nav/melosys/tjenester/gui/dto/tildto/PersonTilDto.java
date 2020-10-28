package no.nav.melosys.tjenester.gui.dto.tildto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;

public class PersonTilDto {

    public PersonTilDto() {}

    public PersonTilDto(PersonDokument person) {
        fnr = person.fnr;
        sivilstand = person.sivilstand;
        statsborgerskap = person.statsborgerskap;
        statsborgerskapDato = person.statsborgerskapDato;
        sammensattNavn = person.sammensattNavn;
        personStatus = person.personstatus;
        kjoenn = person.kjønn;
        foedselsdato = person.fødselsdato;
    }

    public String fnr;

    public Land statsborgerskap;

    public LocalDate statsborgerskapDato;

    public Sivilstand sivilstand;

    public String sammensattNavn;

    public List<Familiemedlem> familiemedlemmer = new ArrayList<>();

    public Personstatus personStatus;

    public KjoennsType kjoenn;

    public LocalDate foedselsdato;

    @JsonProperty(defaultValue = "false" )
    public boolean erEgenAnsatt; // MELOSYS-1580
}
