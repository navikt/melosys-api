package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

    public String fornavn;

    public String mellomnavn;

    public String etternavn;

    public String sammensattNavn;

    public List<Familiemedlem> familiemedlemmer = new ArrayList<>();

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @JsonProperty("foedselsdato")
    public LocalDate fødselsdato;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate dødsdato;

    public Diskresjonskode diskresjonskode;

    public Personstatus personstatus;

    public LocalDate statsborgerskapDato;

    public Bostedsadresse bostedsadresse = new Bostedsadresse();

    public UstrukturertAdresse postadresse = new UstrukturertAdresse();

    public MidlertidigPostadresse midlertidigPostadresse = new MidlertidigPostadresse();

    public UstrukturertAdresse gjeldendePostadresse = new UstrukturertAdresse();

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

    public boolean harBeskyttelsesbehov() {
        return diskresjonskode != null && diskresjonskode.erKode6();
    }
}
