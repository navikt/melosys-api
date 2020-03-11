package no.nav.melosys.integrasjon.eessi.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;

public class Virksomhet {

    private String navn;
    private Adresse adresse;
    private String orgnr;
    private String type; //Trenger kanskje ikke denne?

    public Virksomhet() {
    }

    public Virksomhet(String navn, String orgnr, Adresse adresse) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
    }


    public ForetakUtland tilForetakUtland(boolean erSelvstendig) {
        ForetakUtland foretakUtland = new ForetakUtland();

        foretakUtland.navn = navn;
        foretakUtland.orgnr = orgnr;
        foretakUtland.adresse = adresse.tilStrukturertAdresse();
        foretakUtland.selvstendigNæringsvirksomhet = erSelvstendig;

        return foretakUtland;
    }

    public OrganisasjonDokument tilOrganisasjonDokument() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.organisasjonDetaljer = new OrganisasjonsDetaljer();
        organisasjonDokument.organisasjonDetaljer.navn = new ArrayList<>();
        organisasjonDokument.organisasjonDetaljer.orgnummer = orgnr;
        organisasjonDokument.organisasjonDetaljer.postadresse = List.of(adresse.tilSemistrukturertAdresse());
        organisasjonDokument.navn = List.of(navn);
        return organisasjonDokument;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public void setAdresse(Adresse adresse) {
        this.adresse = adresse;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
