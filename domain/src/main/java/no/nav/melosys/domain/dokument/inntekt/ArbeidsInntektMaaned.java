package no.nav.melosys.domain.dokument.inntekt;

import java.time.YearMonth;
import java.util.List;

public class ArbeidsInntektMaaned {

    private YearMonth aarMaaned;

    private List<Avvik> avvikListe;

    private ArbeidsInntektInformasjon arbeidsInntektInformasjon;

    public YearMonth getAarMaaned() {
        return aarMaaned;
    }

    public void setAarMaaned(YearMonth aarMaaned) {
        this.aarMaaned = aarMaaned;
    }

    public List<Avvik> getAvvikListe() {
        return avvikListe;
    }

    public void setAvvikListe(List<Avvik> avvikListe) {
        this.avvikListe = avvikListe;
    }

    public ArbeidsInntektInformasjon getArbeidsInntektInformasjon() {
        return arbeidsInntektInformasjon;
    }

    public void setArbeidsInntektInformasjon(ArbeidsInntektInformasjon arbeidsInntektInformasjon) {
        this.arbeidsInntektInformasjon = arbeidsInntektInformasjon;
    }
}
