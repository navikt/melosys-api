package no.nav.melosys.domain.dokument.organisasjon;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer;
import no.nav.melosys.domain.kodeverk.Landkoder;

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

    public String getOrgnummer() {
        return orgnummer;
    }

    public List<Organisasjonsnavn> getNavn() {
        return navn;
    }

    public List<GeografiskAdresse> getForretningsadresser() {
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

    public UstrukturertAdresse hentUstrukturertForretningsadresse() {
        GeografiskAdresse adresse = hentFørsteGyldigeForretningsadresse();
        return konverterTilUstrukturertAdresse(adresse);
    }

    public GeografiskAdresse hentFørsteGyldigeForretningsadresse() {
        return hentFørsteGyldigeAdresse(forretningsadresse);
    }

    public GeografiskAdresse hentFørsteGyldigePostadresse() {
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

        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        if (adresse instanceof SemistrukturertAdresse) {
            SemistrukturertAdresse sAdresse = (SemistrukturertAdresse) adresse;
            if (sAdresse.getAdresselinje1() != null) {
                ustrukturertAdresse.adresselinjer.add(sAdresse.getAdresselinje1());
            }
            if (sAdresse.getAdresselinje2() != null) {
                ustrukturertAdresse.adresselinjer.add(sAdresse.getAdresselinje2());
            }
            if (sAdresse.getAdresselinje3() != null) {
                ustrukturertAdresse.adresselinjer.add(sAdresse.getAdresselinje3());
            }
            ustrukturertAdresse.landKode = sAdresse.getLandkode();

            if (!sAdresse.getLandkode().equals(Landkoder.NO.getKode())) {
                ustrukturertAdresse.adresselinjer.add(sAdresse.getPoststedUtland());
            } else {
                ustrukturertAdresse.adresselinjer.add(sAdresse.getPostnr());
            }
        }
        else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw new RuntimeException("Adresse ikke støttet " + adresse.getClass().getSimpleName());
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

            strukturertAdresse.gatenavn = adresseLinje.replaceAll("\\s+", " ");
            strukturertAdresse.landKode = sAdresse.getLandkode();

            if (!strukturertAdresse.landKode.equals(Landkoder.NO.getKode())) {
                strukturertAdresse.postnummer = "";
                strukturertAdresse.poststed = sAdresse.getPoststedUtland();
            } else {
                strukturertAdresse.postnummer = sAdresse.getPostnr();
                strukturertAdresse.poststed = "";
            }
        }
        else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw new RuntimeException("GeografiskAdresse ikke støttet " + adresse.getClass().getSimpleName());
        }
        return strukturertAdresse;
    }
}
