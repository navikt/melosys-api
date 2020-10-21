package no.nav.melosys.domain.eessi.sed;

import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.ForetakUtland;
import no.nav.melosys.domain.eessi.SedOrganisasjon;
import org.apache.commons.lang3.StringUtils;

public class Virksomhet {
    private static final String UKJENT = "Unknown";

    private String navn;
    private Adresse adresse;
    private String orgnr;

    public Virksomhet() {
    }

    public Virksomhet(String navn, String orgnr, Adresse adresse) {
        this.navn = navn;
        this.orgnr = StringUtils.isBlank(orgnr) ? UKJENT : orgnr;
        this.adresse = adresse;
    }

    public ForetakUtland tilForetakUtland() {
        return tilForetakUtland(false);
    }

    public ForetakUtland tilSelvstendigForetakUtland() {
        return tilForetakUtland(true);
    }

    private ForetakUtland tilForetakUtland(boolean erSelvstendig) {
        ForetakUtland foretakUtland = new ForetakUtland();

        foretakUtland.navn = navn;
        foretakUtland.orgnr = orgnr;
        foretakUtland.adresse = adresse.tilStrukturertAdresse();
        foretakUtland.selvstendigNæringsvirksomhet = erSelvstendig;

        return foretakUtland;
    }

    public SedOrganisasjon tilOrganisasjon() {
        SedOrganisasjon organisasjon = new SedOrganisasjon();

        organisasjon.setOrgnummer(orgnr);
        organisasjon.setNavn(navn);

        if (adresse.getAdressetype() == Adressetype.POSTADRESSE) {
            organisasjon.setPostadresse(adresse.tilStrukturertAdresse());
        } else {
            organisasjon.setForretningsadresse(adresse.tilStrukturertAdresse());
        }

        return organisasjon;
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

    public String hentOrgnrEllerNavn() {
        return StringUtils.isNotEmpty(orgnr) ? orgnr : navn;
    }
}
