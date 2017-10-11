package no.nav.melosys.domain.dokument.inntekt;

import java.time.YearMonth;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.YearMonthTimeZoneXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArbeidsInntektMaaned {

    @XmlJavaTypeAdapter(YearMonthTimeZoneXmlAdapter.class)
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
