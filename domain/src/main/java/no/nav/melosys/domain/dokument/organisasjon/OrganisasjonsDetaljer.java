package no.nav.melosys.domain.dokument.organisasjon;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonsDetaljer {

    private String orgnummer;

    @XmlElement(name="organisasjonsnavn")
    private List<Organisasjonsnavn> navn;

    private List<GeografiskAdresse> forretningsadresse;

    private List<GeografiskAdresse> postadresse;

    public String getOrgnummer() {
        return orgnummer;
    }

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    public List<Organisasjonsnavn> getNavn() {
        return navn;
    }

    public void setNavn(List<Organisasjonsnavn> navn) {
        this.navn = navn;
    }

    public List<GeografiskAdresse> getForretningsadresse() {
        return forretningsadresse;
    }

    public void setForretningsadresse(List<GeografiskAdresse> forretningsadresse) {
        this.forretningsadresse = forretningsadresse;
    }

    public List<GeografiskAdresse> getPostadresse() {
        return postadresse;
    }

    public void setPostadresse(List<GeografiskAdresse> postadresse) {
        this.postadresse = postadresse;
    }
}
