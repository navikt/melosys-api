package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;

public class Person {

    public String fnr;

    public Sivilstand sivilstand;

    public LocalDate sivilstandGyldighetsperiodeFom;

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

    public List<Familiemedlem> hentBarn() {
        return hentFamiliemedlemmerMedFilter(Familiemedlem::erBarn).collect(Collectors.toList());
    }

    public List<Familiemedlem> hentForeldre() {
        return hentFamiliemedlemmerMedFilter(Familiemedlem::erForelder).collect(Collectors.toList());
    }

    public Optional<Familiemedlem> hentEktefelleSamboerPartner() {
        return hentFamiliemedlemmerMedFilter(Familiemedlem::erEktefellePartnerSamboer).findAny();
    }

    private Stream<Familiemedlem> hentFamiliemedlemmerMedFilter(Predicate<Familiemedlem> filter) {
        return familiemedlemmer.stream().filter(filter);
    }
}
