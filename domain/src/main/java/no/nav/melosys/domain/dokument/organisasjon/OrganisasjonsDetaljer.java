package no.nav.melosys.domain.dokument.organisasjon;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonsDetaljer {

    private String orgnummer;

    @XmlElement(name="organisasjonsnavn")
    private List<Organisasjonsnavn> navn;

    private List<GeografiskAdresse> forretningsadresse;

    private List<GeografiskAdresse> postadresse;

    private List<Telefonnummer> telefon;

    private List<Epost> epostadresse;

    private List<String> naering; //"http://nav.no/kodeverk/Kodeverk/Næringskoder"

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

    public List<Telefonnummer> getTelefon() {
        return telefon;
    }

    public void setTelefon(List<Telefonnummer> telefon) {
        this.telefon = telefon;
    }

    public List<Epost> getEpostadresse() {
        return epostadresse;
    }

    public void setEpostadresse(List<Epost> epostadresse) {
        this.epostadresse = epostadresse;
    }

    public List<String> getNaering() {
        return naering;
    }

    public void setNaering(List<String> naering) {
        this.naering = naering;
    }
}
