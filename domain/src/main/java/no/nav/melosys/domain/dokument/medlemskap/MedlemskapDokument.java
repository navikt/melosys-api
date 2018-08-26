package no.nav.melosys.domain.dokument.medlemskap;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MedlemskapDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name="medlemsperiode")
    @XmlElement(name="medlemsperiode")
    public List<Medlemsperiode> medlemsperiode = new ArrayList<>();

    public List<Medlemsperiode> getMedlemsperiode() {
        return medlemsperiode;
    }

}
