package no.nav.melosys.domain.dokument.medlemskap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

import static java.util.function.Predicate.not;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MedlemskapDokument implements SaksopplysningDokument {

    @XmlElementWrapper(name="medlemsperiode")
    @XmlElement(name="medlemsperiode")
    public List<Medlemsperiode> medlemsperiode = new ArrayList<>();

    public List<Medlemsperiode> getMedlemsperiode() {
        return medlemsperiode;
    }

    public List<Medlemsperiode> hentMedlemsperioderKildeIkkeLånekassen() {
        return medlemsperiode.stream()
            .filter(not(Medlemsperiode::erKildeLånekassen))
            .collect(Collectors.toList());
    }
}
