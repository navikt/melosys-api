package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ArbeidsforholdDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name="arbeidsforhold")
    @XmlElement(name="arbeidsforhold")
    List<Arbeidsforhold> arbeidsforhold;

    @JsonValue
    public List<Arbeidsforhold> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<Arbeidsforhold> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }
}
