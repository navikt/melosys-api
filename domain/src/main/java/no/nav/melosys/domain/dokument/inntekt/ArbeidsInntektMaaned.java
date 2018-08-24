package no.nav.melosys.domain.dokument.inntekt;

import java.time.YearMonth;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.dokument.jaxb.YearMonthTimeZoneXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArbeidsInntektMaaned {

    @XmlJavaTypeAdapter(YearMonthTimeZoneXmlAdapter.class)
    public YearMonth aarMaaned;

    @JsonIgnore
    public List<Avvik> avvikListe;

    public ArbeidsInntektInformasjon arbeidsInntektInformasjon;

    public ArbeidsInntektInformasjon getArbeidsInntektInformasjon() {
        return arbeidsInntektInformasjon;
    }

}
