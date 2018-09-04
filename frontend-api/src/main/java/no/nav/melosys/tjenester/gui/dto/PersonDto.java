package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;

public class PersonDto {

    public String fnr;

    public Sivilstand sivilstand;

    /** Kodeverk: Landkoder */
    public Land statsborgerskap;

    /** Kodeverk: Kjønnstyper */
    @JsonProperty("kjoenn")
    public KjoennsType kjønn;

    public String sammensattNavn;

    public List<Familiemedlem> familiemedlemmer = new ArrayList<>();

    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @JsonIgnore // TODO må avklares
    public LocalDate dødsdato;

    public Diskresjonskode diskresjonskode;

    @JsonProperty("personStatus")
    public Personstatus personstatus;

    public LocalDate statsborgerskapDato;

    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    @JsonIgnore
    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

    @JsonIgnore
    public MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();

    @JsonProperty(defaultValue = "false" )
    public boolean erEgenAnsatt; // FIXME : MELOSYS-1580

    public PersonDto() {}

    public PersonDto(PersonDokument personDokument) {
        this.fnr = personDokument.fnr;
        this.sivilstand = personDokument.sivilstand;
        this.statsborgerskap = personDokument.statsborgerskap;
        this.kjønn = personDokument.kjønn;
        this.sammensattNavn = personDokument.sammensattNavn;
        this.familiemedlemmer = personDokument.familiemedlemmer;
        this.fødselsdato = personDokument.fødselsdato;
        this.dødsdato = personDokument.dødsdato;
        this.diskresjonskode = personDokument.diskresjonskode;
        this.personstatus = personDokument.personstatus;
        this.bostedsadresse = personDokument.bostedsadresse;
        this.postadresse = personDokument.postadresse;
        this.midlertidigPostadresse = personDokument.midlertidigPostadresse;
        this.erEgenAnsatt = personDokument.erEgenAnsatt;
    }

}
