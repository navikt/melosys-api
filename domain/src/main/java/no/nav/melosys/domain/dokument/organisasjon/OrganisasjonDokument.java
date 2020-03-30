package no.nav.melosys.domain.dokument.organisasjon;

import java.time.LocalDate;
import java.util.List;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.Organisasjon;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

// N.B. Denne klassen serialiseres i OrganisasjonSerializer

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonDokument implements SaksopplysningDokument, Organisasjon {

    private String orgnummer;

    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    public List<String> navn;

    public OrganisasjonsDetaljer organisasjonDetaljer;

    public String sektorkode; //"http://nav.no/kodeverk/Kodeverk/Sektorkoder"

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate oppstartsdato;
    
    public String enhetstype; //"http://nav.no/kodeverk/Kodeverk/EnhetstyperJuridiskEnhet"

    @Override
    public String getOrgnummer() {
        return orgnummer;
    }

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    @Override
    public String getNavn() {
        return lagSammenslåttNavn();
    }

    @Override
    public StrukturertAdresse getForretningsadresse() {
        return organisasjonDetaljer.hentStrukturertForretningsadresse();
    }

    @Override
    public StrukturertAdresse getPostadresse() {
        return organisasjonDetaljer.hentStrukturertPostadresse();
    }

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    public String lagSammenslåttNavn() {
        return navn == null ? "UKJENT" : String.join(" ", navn);
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

    @Override
    public LocalDate getOppstartsdato() {
        return oppstartsdato;
    }

    public void setOppstartsdato(LocalDate oppstartsdato) {
        this.oppstartsdato = oppstartsdato;
    }

    @Override
    public String getEnhetstype() {
        return enhetstype;
    }

    public void setEnhetstype(String enhetstype) {
        this.enhetstype = enhetstype;
    }
}
