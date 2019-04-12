package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public class PersonDokument extends SaksopplysningDokument {

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

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @JsonIgnore
    public LocalDate dødsdato;

    @JsonIgnore
    public Diskresjonskode diskresjonskode;

    @JsonProperty("personStatus")
    public Personstatus personstatus;

    public LocalDate statsborgerskapDato;

    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    @JsonIgnore
    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

    public MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();

    @XmlTransient
    @JsonProperty(defaultValue = "false" )
    public boolean erEgenAnsatt; // MELOSYS-1580

}
