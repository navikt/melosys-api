package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;

public class Personopplysning {

    public String fnr;

    public Sivilstand sivilstand;

    /** Kodeverk: Landkoder */
    public Land statsborgerskap;

    /** Kodeverk: Kjønnstyper */
    @JsonProperty("kjoenn")
    public KjoennsType kjønn;

    @JsonIgnore
    public String fornavn;

    @JsonIgnore
    public String mellomnavn;

    @JsonIgnore
    public String etternavn;

    public String sammensattNavn;

    public List<Familiemedlem> familiemedlemmer = new ArrayList<>();

    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @JsonIgnore
    public LocalDate dødsdato;

    @JsonIgnore
    public Diskresjonskode diskresjonskode;

    @JsonProperty("personStatus")
    public Personstatus personstatus;

    public LocalDate statsborgerskapDato;

    @JsonIgnore
    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    @JsonIgnore
    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

    @JsonIgnore
    public MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();

}
