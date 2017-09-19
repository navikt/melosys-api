package no.nav.melosys.domain.dokument.person;

import java.time.LocalDate;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

/**
 * Representerer svar fra personregisteret (TPS)
 * 
 * TODO (Farjam 2017-09-19): Trenger revisjon, se EESSI2-279.
 *  
 */
@XmlRootElement
public class PersonopplysningDokument extends SaksopplysningDokument {

    public String fnr;

    public Sivilstand sivilstand;

    /** Kodeverk: Landkoder */
    public String statsborgerskap;

    /** Kodeverk: Kjønnstyper */
    public String kjønn;

    public String sammensattNavn;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate fødselsdato;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate dødsdato;

    public Diskresjonskode diskresjonskode;

    public Personstatus personstatus;
    
}
