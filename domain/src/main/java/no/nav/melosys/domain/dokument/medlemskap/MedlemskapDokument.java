package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MedlemskapDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name="medlemsperiode")
    @XmlElement(name="medlemsperiode")
    public List<Medlemsperiode> medlemsperiode;

    public List<Medlemsperiode> getMedlemsperiode() {
        return medlemsperiode;
    }

}
