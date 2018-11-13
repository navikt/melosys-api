package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;

public class PersonDto {

    public PersonDto(PersonDokument person) {
        fnr = person.fnr;
        sivilstand = person.sivilstand;
        statsborgerskap = person.statsborgerskap;
        sammensattNavn = person.sammensattNavn;
        personstatus = person.personstatus;
        kjoenn = person.kjønn;
        foedselsdato = person.fødselsdato;
        bostedsadresse = person.bostedsadresse;
    }

    public String fnr;

    public Land statsborgerskap;

    public Sivilstand sivilstand;

    public String sammensattNavn;

    public Personstatus personstatus;

    public KjoennsType kjoenn;

    public LocalDate foedselsdato;

    public Bostedsadresse bostedsadresse;
}
