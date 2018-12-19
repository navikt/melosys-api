package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import org.apache.commons.lang3.StringUtils;

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

    public Set<String> hentOrgnumre() {
        return getArbeidsforhold().stream()
            .flatMap(arbeidsforhold -> Stream.of(arbeidsforhold.getArbeidsgiverID(), arbeidsforhold.getOpplysningspliktigID()))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());
    }

    public Set<Periode> hentAnsettelsesperioder(Collection<String> orgnummere) {
        return getArbeidsforhold().stream()
                .filter(arbeidsforhold -> orgnummere.contains(arbeidsforhold.arbeidsgiverID))
                .map(arbeidsforhold -> arbeidsforhold.getAnsettelsesPeriode())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
