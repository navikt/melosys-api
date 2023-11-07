package no.nav.melosys.domain.dokument.organisasjon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.AbstraktOrganisasjon;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;

import javax.xml.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonDokument extends AbstraktOrganisasjon implements SaksopplysningDokument {
    @XmlElementWrapper(name = "navn")
    @XmlElement(name = "navnelinje")
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
    @JsonView(DokumentView.FrontendApi.class)
    public StrukturertAdresse getForretningsadresse() {
        if (organisasjonDetaljer == null) return null;

        return organisasjonDetaljer.hentStrukturertForretningsadresse();
    }

    @Override
    @JsonView(DokumentView.FrontendApi.class)
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
        this.navn = new ArrayList<>(List.of(navn));
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

    public boolean harRegistrertPostadresse() {
        return getPostadresse() != null && getPostadresse().erGyldig();
    }

    public boolean harRegistrertForretningsadresse() {
        return getForretningsadresse() != null && getForretningsadresse().erGyldig();
    }

    public StrukturertAdresse hentTilgjengeligAdresse() {
        return harRegistrertPostadresse() ? getPostadresse() : getForretningsadresse();
    }

    public boolean harRegistrertAdresse() {
        return (harRegistrertPostadresse() || harRegistrertForretningsadresse());
    }
}
