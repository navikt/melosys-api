package no.nav.melosys.domain.avklartefakta;

import java.time.LocalDate;
import no.nav.melosys.domain.adresse.Adresse;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;

public class AvklartVirksomhet {

    public final String navn;
    public final String orgnr;
    public final Adresse adresse;
    public final boolean adresseErOgsåArbeidssted;
    public final Yrkesaktivitetstyper yrkesaktivitet;
    public final LocalDate opphoersdato;

    public AvklartVirksomhet(ForetakUtland foretak) {
        this.navn = foretak.getNavn();
        this.orgnr = foretak.getOrgnr();
        this.adresse = foretak.getAdresse();
        this.adresseErOgsåArbeidssted = false;
        this.yrkesaktivitet = (Boolean.TRUE.equals(foretak.getSelvstendigNæringsvirksomhet())) ?
            Yrkesaktivitetstyper.SELVSTENDIG : Yrkesaktivitetstyper.LOENNET_ARBEID;
        this.opphoersdato = null;
    }

    public AvklartVirksomhet(String navn, String orgnr, Adresse adresse, Yrkesaktivitetstyper yrkesaktivitet) {
        this(navn, orgnr, adresse, yrkesaktivitet, null);
    }

    public AvklartVirksomhet(String navn, String orgnr, Adresse adresse, Yrkesaktivitetstyper yrkesaktivitet, LocalDate opphoersdato) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
        this.adresseErOgsåArbeidssted = false;
        this.yrkesaktivitet = yrkesaktivitet;
        this.opphoersdato = opphoersdato;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public boolean erSelvstendigForetak() {
        return yrkesaktivitet == Yrkesaktivitetstyper.SELVSTENDIG;
    }

    public boolean erArbeidsgiver() {
        return yrkesaktivitet == Yrkesaktivitetstyper.LOENNET_ARBEID;
    }

    public boolean erOpphoert() {
        return opphoersdato != null && opphoersdato.isBefore(LocalDate.now());
    }
}
