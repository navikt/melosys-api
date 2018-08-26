package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ArbeidsforholdDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name="arbeidsforhold")
    @XmlElement(name="arbeidsforhold")
    public List<Arbeidsforhold> arbeidsforhold = new ArrayList<>();

    public ArbeidsforholdDokument() {}

    @JsonCreator
    public ArbeidsforholdDokument(List<Arbeidsforhold> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    @JsonValue
    public List<Arbeidsforhold> getArbeidsforhold() {
        return arbeidsforhold;
    }

}
