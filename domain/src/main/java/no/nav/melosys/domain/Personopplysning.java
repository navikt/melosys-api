package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.jpa.kodeverk.DiskresjonskodeConverter;
import no.nav.melosys.domain.jpa.kodeverk.KjoennsTypeConverter;
import no.nav.melosys.domain.jpa.kodeverk.SivilstandConverter;

@Entity
@Table(name = "personopplysning")
public class Personopplysning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne
    @JoinColumn(name = "behandling_id")
    public Behandling behandling;

    @Column(nullable = false)
    public String fnr;

    @Convert(converter = SivilstandConverter.class)
    public Sivilstand sivilstand;

    @Transient
    public LocalDate sivilstandGyldighetsperiodeFom;

    /** Kodeverk: Landkoder */
    @Transient // FIXME
    public Land statsborgerskap;

    /** Kodeverk: Kjønnstyper */
    @Column(name = "kjoenn")
    @Convert(converter = KjoennsTypeConverter.class)
    @JsonProperty("kjoenn")
    public KjoennsType kjønn;

    @JsonIgnore
    public String fornavn;

    @JsonIgnore
    public String mellomnavn;

    @JsonIgnore
    public String etternavn;

    @Column(name = "sammensatt_navn")
    public String sammensattNavn;

    @OneToMany(mappedBy = "personopplysning", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    public List<Familiemedlem> familiemedlemmer = new ArrayList<>();

    @Column(name = "foedselsdato")
    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @Column(name = "doedsdato")
    @JsonIgnore
    public LocalDate dødsdato;

    @Convert(converter = DiskresjonskodeConverter.class)
    @JsonIgnore
    public Diskresjonskode diskresjonskode;

    @Enumerated(EnumType.STRING)
    @JsonProperty("personStatus")
    public Personstatus personstatus;

    @Transient
    public LocalDate statsborgerskapDato;

    @Transient
    @JsonIgnore
    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    @Transient
    @JsonIgnore
    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

    @Transient
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
