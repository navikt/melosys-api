package no.nav.melosys.domain.dokument.organisasjon;

import java.time.LocalDate;
import java.util.List;
import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.AbstraktOrganisasjon;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(value = {"postadresse", "forretningsadresse"}, allowGetters = true)
public class OrganisasjonDokument  extends AbstraktOrganisasjon implements SaksopplysningDokument {
    @XmlElementWrapper(name="navn")
    @XmlElement(name="navnelinje")
    public List<String> navn;

    public OrganisasjonsDetaljer organisasjonDetaljer;

    public String sektorkode; //"http://nav.no/kodeverk/Kodeverk/Sektorkoder"

    public void setOrgnummer(String orgnummer) {
        this.orgnummer = orgnummer;
    }

    @Override
    public String getNavn() {
        return lagSammenslåttNavn();
    }

    @Override
    public StrukturertAdresse getForretningsadresse() {
        if (organisasjonDetaljer == null) return null;

        return organisasjonDetaljer.hentStrukturertForretningsadresse();
    }

    @Override
    public StrukturertAdresse getPostadresse() {
        if (organisasjonDetaljer == null) return null;

        return organisasjonDetaljer.hentStrukturertPostadresse();
    }

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    public String lagSammenslåttNavn() {
        return navn == null ? "UKJENT" : String.join(" ", navn);
    }

    public void setNavn(List<String> navn) {
        this.navn = navn;
    }

    // Brukes til å deserialisere objektet fra databasen
    @JsonProperty("navn")
    public void setNavn(String navn) {
        this.navn = List.of(navn);
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

    public void setOppstartsdato(LocalDate oppstartsdato) {
        this.oppstartsdato = oppstartsdato;
    }

    public void setEnhetstype(String enhetstype) {
        this.enhetstype = enhetstype;
    }
}
