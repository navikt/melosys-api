package no.nav.melosys.domain.dokument.organisasjon;

import java.time.LocalDate;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonDokument extends SaksopplysningDokument {

    private String orgnummer;

    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    public List<String> navn;

    public OrganisasjonsDetaljer organisasjonDetaljer;

    public String sektorkode; //"http://nav.no/kodeverk/Kodeverk/Sektorkoder"

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate oppstartsdato;

    public String enhetstype; //"http://nav.no/kodeverk/Kodeverk/EnhetstyperJuridiskEnhet"

    public String getOrgnummer() {
        return orgnummer;
    }

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    public List<String> getNavn() {
        return navn;
    }

    public void setNavn(List<String> navn) {
        this.navn = navn;
    }

    public OrganisasjonsDetaljer getOrganisasjonDetaljer() {
        return organisasjonDetaljer;
    }

    public void setOrganisasjonDetaljer(OrganisasjonsDetaljer organisasjonDetaljer) {
        this.organisasjonDetaljer = organisasjonDetaljer;
    }

    public String getSektorkode() {
        return sektorkode;
    }

    public void setSektorkode(String sektorkode) {
        this.sektorkode = sektorkode;
    }

    public LocalDate getOppstartsdato() {
        return oppstartsdato;
    }

    public void setOppstartsdato(LocalDate oppstartsdato) {
        this.oppstartsdato = oppstartsdato;
    }

    public String getEnhetstype() {
        return enhetstype;
    }

    public void setEnhetstype(String enhetstype) {
        this.enhetstype = enhetstype;
    }
}
