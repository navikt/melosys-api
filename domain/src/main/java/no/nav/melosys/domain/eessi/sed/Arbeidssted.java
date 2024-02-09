package no.nav.melosys.domain.eessi.sed;

import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import org.apache.commons.lang3.StringUtils;

public class Arbeidssted {
    private static final String IKKE_TILGJENGELIG = "N/A";

    private String navn;
    private Adresse adresse;
    private boolean fysisk;
    private String hjemmebase;

    public static Arbeidssted lagIkkeFastArbeidssted(String landkode) {
        Arbeidssted arbeidssted = new Arbeidssted();
        arbeidssted.setNavn(Adresse.INGEN_FAST_ADRESSE);
        arbeidssted.setAdresse(Adresse.lagIkkeFastAdresse(landkode));
        return arbeidssted;
    }

    public FysiskArbeidssted tilFysiskArbeidssted() {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();

        fysiskArbeidssted.setAdresse(adresse.tilStrukturertAdresse());
        fysiskArbeidssted.setVirksomhetNavn(navn);

        return fysiskArbeidssted;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = StringUtils.isBlank(navn) ? IKKE_TILGJENGELIG : navn;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public void setAdresse(Adresse adresse) {
        this.adresse = adresse;
    }

    public boolean isFysisk() {
        return fysisk;
    }

    public void setFysisk(boolean fysisk) {
        this.fysisk = fysisk;
    }

    public String getHjemmebase() {
        return hjemmebase;
    }

    public void setHjemmebase(String hjemmebase) {
        this.hjemmebase = hjemmebase;
    }
}
