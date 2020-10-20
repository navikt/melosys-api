package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;

// FIXME: Lag PersonDto for frontend (denne brukes til lagring)
public class Person {

    public String fnr;

    public Sivilstand sivilstand;

    public LocalDate sivilstandGyldighetsperiodeFom;

    /** Kodeverk: Landkoder */
    public Land statsborgerskap;

    /** Kodeverk: Kjønnstyper */
    @JsonProperty("kjoenn")
    public KjoennsType kjønn;

    public String fornavn;

    public String mellomnavn;

    public String etternavn;

    public String sammensattNavn;

    public List<Familiemedlem> familiemedlemmer = new ArrayList<>();

    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @JsonProperty("doedsdato")
    public LocalDate dødsdato;

    public Diskresjonskode diskresjonskode;

    public Personstatus personstatus;

    public LocalDate statsborgerskapDato;

    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

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

    public Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder) {
        return hentForeldre().stream()
            .filter(forelder -> !fnrGjeldendeForelder.equals(forelder.fnr))
            .findAny();
    }

    private Stream<Familiemedlem> hentFamiliemedlemmerMedFilter(Predicate<Familiemedlem> filter) {
        return familiemedlemmer.stream().filter(filter);
    }
}
