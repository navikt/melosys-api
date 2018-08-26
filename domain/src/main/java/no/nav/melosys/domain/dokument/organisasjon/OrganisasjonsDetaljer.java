package no.nav.melosys.domain.dokument.organisasjon;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonsDetaljer {

    public String orgnummer;

    @XmlElement(name="organisasjonsnavn")
    public List<Organisasjonsnavn> navn = new ArrayList<>();

    public List<GeografiskAdresse> forretningsadresse = new ArrayList<>();

    private List<GeografiskAdresse> postadresse = new ArrayList<>();

    public List<Telefonnummer> telefon = new ArrayList<>();

    public List<Epost> epostadresse = new ArrayList<>();

    public List<String> naering = new ArrayList<>(); //"http://nav.no/kodeverk/Kodeverk/Næringskoder"

    public String getOrgnummer() {
        return orgnummer;
    }

    public List<Organisasjonsnavn> getNavn() {
        return navn;
    }

    public List<GeografiskAdresse> getForretningsadresse() {
        return forretningsadresse;
    }

    public List<GeografiskAdresse> getPostadresse() {
        return postadresse;
    }

    public List<Telefonnummer> getTelefon() {
        return telefon;
    }

    public List<Epost> getEpostadresse() {
        return epostadresse;
    }

    public List<String> getNaering() {
        return naering;
    }

}
