package no.nav.melosys.domain.dokument.organisasjon;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonDokument extends SaksopplysningDokument {

    private String orgnummer;

    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    private List<String> navn;

    private OrganisasjonsDetaljer organisasjonDetaljer;

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

}
