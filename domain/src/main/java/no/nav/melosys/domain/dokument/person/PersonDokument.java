package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

/**
 * Representerer svar fra personregisteret (TPS)
 * 
 * TODO (Farjam 2017-09-19): Trenger revisjon, se EESSI2-279.
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
    public String kjønn;

    public String sammensattNavn;

    public List<Familiemedlem> familiemedlemmer;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @JsonIgnore // TODO må avklares
    public LocalDate dødsdato;

    public Diskresjonskode diskresjonskode;

    // TODO trenger vi den?
    @JsonIgnore
    public Personstatus personstatus;

    public Bostedsadresse bostedsadresse;

    public UstrukturertAdresse postadresse;

    public MidlertidigPostadresse midlertidigPostadresse;

}
