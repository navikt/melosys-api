package no.nav.melosys.domain.dokument.organisasjon;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost;
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer;

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

    public UstrukturertAdresse getForretningsadresseUstrukturert() {
        GeografiskAdresse adresse = hentFørsteGyldigeAdresse(forretningsadresse);
        return konverterTilUstrukturertAdresse(adresse);
    }

    public StrukturertAdresse getForretningsadresseStrukturert() {
        GeografiskAdresse adresse = hentFørsteGyldigeAdresse(forretningsadresse);
        return konverterTilStrukturertAdresse(adresse);
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
        UstrukturertAdresse ustrukturertAdresse = new UstrukturertAdresse();
        Gateadresse gateadresse = (Gateadresse)adresse;
        ustrukturertAdresse.adresselinjer.add(gateadresse.getGatenavn());
        ustrukturertAdresse.adresselinjer.add(gateadresse.getHusnummer()+gateadresse.getHusbokstav());

        if (adresse instanceof SemistrukturertAdresse) {
            ustrukturertAdresse.adresselinjer.add(((SemistrukturertAdresse) adresse).getPostnr());
        }
        ustrukturertAdresse.adresselinjer.add(gateadresse.getPoststed());
        ustrukturertAdresse.landKode = adresse.getLandkode();

        return ustrukturertAdresse;
    }

    private StrukturertAdresse konverterTilStrukturertAdresse(GeografiskAdresse adresse) {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        Gateadresse gateadresse = (Gateadresse)adresse;
        strukturertAdresse.gatenavn = gateadresse.getGatenavn();
        strukturertAdresse.husnummer = gateadresse.getHusnummer() + gateadresse.getHusbokstav();
        strukturertAdresse.poststed = gateadresse.getPoststed();
        strukturertAdresse.landKode = gateadresse.getLandkode();

        if (adresse instanceof SemistrukturertAdresse) {
            strukturertAdresse.postnummer = ((SemistrukturertAdresse) adresse).getPostnr();
        }
        return strukturertAdresse;
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
