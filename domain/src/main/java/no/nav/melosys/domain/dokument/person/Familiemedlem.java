package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.Personopplysning;

@Entity
@Table(name = "familiemedlem")
public class Familiemedlem {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "personopplysning_id")
    @XmlTransient // TODO: Fjernes når PersonDokument fjernes
    public Personopplysning personopplysning;

    public String fnr;

    @JsonProperty("sammensattNavn")
    public String navn;

    @Enumerated(EnumType.STRING)
    @JsonProperty("relasjonstype")
    public Familierelasjon familierelasjon;

    @JsonIgnore
    @Column(name = "foedselsdato")
    public LocalDate fødselsdato;

    @JsonIgnore
    @Column(name = "bor_med_bruker")
    public boolean borMedBruker;

    @JsonIgnore
    @Transient // FIXME
    public Sivilstand sivilstand;

    @JsonIgnore
    @Column(name = "sivilstand_fom")
    public LocalDate sivilstandGyldighetsperiodeFom;

    @JsonIgnore
    @Column(name = "fnr_annen_forelder")
    public String fnrAnnenForelder;

    public boolean erBarn() {
        return familierelasjon == Familierelasjon.BARN;
    }

    public boolean erForelder() {
        return familierelasjon == Familierelasjon.FARA
            || familierelasjon == Familierelasjon.MORA;
    }

    public boolean erEktefellePartnerSamboer() {
        return familierelasjon == Familierelasjon.EKTE
            || familierelasjon == Familierelasjon.REPA
            || familierelasjon == Familierelasjon.SAM;
    }
}
