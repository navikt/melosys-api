package no.nav.melosys.domain.dokument.organisasjon;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;
import no.nav.melosys.domain.dokument.jaxb.OffsetDateTimeToLocalDateXmlAdapter;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer;
import org.springframework.util.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisasjonsDetaljer {

    public String orgnummer;

    @XmlElement(name="organisasjonsnavn")
    public List<Organisasjonsnavn> navn = new ArrayList<>();

    public List<GeografiskAdresse> forretningsadresse = new ArrayList<>();

    public List<GeografiskAdresse> postadresse = new ArrayList<>();

    public List<Telefonnummer> telefon = new ArrayList<>();

    public List<Epost> epostadresse = new ArrayList<>();

    public List<String> naering = new ArrayList<>(); //"http://nav.no/kodeverk/Kodeverk/Næringskoder"

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate opphoersdato = null;

    public String getOrgnummer() {
        return orgnummer;
    }

    public List<Organisasjonsnavn> getNavn() {
        return navn;
    }

    List<GeografiskAdresse> getForretningsadresser() {
        return forretningsadresse;
    }

    public StrukturertAdresse hentStrukturertPostadresse() {
        GeografiskAdresse adresse = hentFørsteGyldigePostadresse();
        return konverterTilStrukturertAdresse(adresse);
    }

    public StrukturertAdresse hentStrukturertForretningsadresse() {
        GeografiskAdresse adresse = hentFørsteGyldigeForretningsadresse();
        return konverterTilStrukturertAdresse(adresse);
    }

    UstrukturertAdresse hentUstrukturertForretningsadresse() {
        GeografiskAdresse adresse = hentFørsteGyldigeForretningsadresse();
        return konverterTilUstrukturertAdresse(adresse);
    }

    private GeografiskAdresse hentFørsteGyldigeForretningsadresse() {
        return hentFørsteGyldigeAdresse(forretningsadresse);
    }

    private GeografiskAdresse hentFørsteGyldigePostadresse() {
        return hentFørsteGyldigeAdresse(postadresse);
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

    public LocalDate getOpphoersdato() {
        return opphoersdato;
    }

    private GeografiskAdresse hentFørsteGyldigeAdresse(List<GeografiskAdresse> adresser) {
        for (GeografiskAdresse adresse : adresser) {
            Periode gyldighetsperiode = adresse.getGyldighetsperiode();
            if (gyldighetsperiode.erGyldig()) {
                return adresse;
            }
        }
        return null;
    }

    private UstrukturertAdresse konverterTilUstrukturertAdresse(GeografiskAdresse adresse) {
        if(adresse == null) {
            return null;
        }

        UstrukturertAdresse ustrukturertAdresse;
        if (adresse instanceof SemistrukturertAdresse) {
            SemistrukturertAdresse sAdresse = (SemistrukturertAdresse) adresse;
            ustrukturertAdresse = UstrukturertAdresse.av(sAdresse);
        }
        else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw new IllegalArgumentException("Adresse ikke støttet " + adresse.getClass().getSimpleName());
        }
        return ustrukturertAdresse;
    }

    private StrukturertAdresse konverterTilStrukturertAdresse(GeografiskAdresse adresse) {
        if(adresse == null) {
            return null;
        }

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        if (adresse instanceof SemistrukturertAdresse) {
            SemistrukturertAdresse sAdresse = (SemistrukturertAdresse) adresse;

            StringBuilder stringBuilder = new StringBuilder();
            if (sAdresse.getAdresselinje1() != null) {
                stringBuilder.append(sAdresse.getAdresselinje1());
            }
            if (sAdresse.getAdresselinje2() != null) {
                stringBuilder.append(" ");
                stringBuilder.append(sAdresse.getAdresselinje2());
            }
            if (sAdresse.getAdresselinje3() != null) {
                stringBuilder.append(" ");
                stringBuilder.append(sAdresse.getAdresselinje3());
            }
            String adresseLinje = stringBuilder.toString();

            strukturertAdresse.setGatenavn(adresseLinje.replaceAll("\\s+", " "));
            strukturertAdresse.setLandkode(sAdresse.getLandkode());
            strukturertAdresse.setPostnummer(sAdresse.getPostnr());

            if (sAdresse.erUtenlandsk()) {
                strukturertAdresse.setPoststed(
                        StringUtils.isEmpty(sAdresse.getPoststedUtland()) ? sAdresse.getPoststed() : sAdresse.getPoststedUtland());
                // Utenlandsk adresse kan ha postnummer som en del av poststed
                if (strukturertAdresse.getPostnummer() == null) {
                    strukturertAdresse.setPostnummer(" ");
                }
            } else {
                strukturertAdresse.setPoststed(sAdresse.getPoststed() == null ? "" : sAdresse.getPoststed());
            }
        }
        else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw new IllegalArgumentException("GeografiskAdresse ikke støttet " + adresse.getClass().getSimpleName());
        }
        return strukturertAdresse;
    }
}
