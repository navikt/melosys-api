package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;


/**
 * Representerer svar fra personregisteret (TPS)
 *
 */
@XmlRootElement
public class PersonDokument implements SaksopplysningDokument {

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

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    // @JsonIgnore // FIXME - Bruk PersonDto i BehandlingTjeneste
    public LocalDate dødsdato;

    // @JsonIgnore // FIXME - Bruk PersonDto i BehandlingTjeneste
    public Diskresjonskode diskresjonskode;

    // @JsonProperty("personStatus") // FIXME - Bruk PersonDto i BehandlingTjeneste
    public Personstatus personstatus;

    public LocalDate statsborgerskapDato;

    // @JsonIgnore // FIXME - Bruk PersonDto i BehandlingTjeneste
    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    // @JsonIgnore // FIXME - Bruk PersonDto i BehandlingTjeneste
    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

    // @JsonIgnore // FIXME - Bruk PersonDto i BehandlingTjeneste
    public MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();

    @XmlTransient
    @JsonProperty(defaultValue = "false" )
    public boolean erEgenAnsatt; // MELOSYS-1580

    public boolean harIkkeRegistrertAdresse() {
        return bostedsadresse.erTom() &&
            postadresse.erTom() &&
            midlertidigPostadresse.land == null;
    }

    public boolean manglerBostedsadresse() {
        return bostedsadresse.erTom();
    }

    public Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder) {
        return familiemedlemmer.stream()
            .filter(Familiemedlem::erForelder)
            .filter(forelder -> !fnrGjeldendeForelder.equals(forelder.fnr))
            .findAny();
    }
}
