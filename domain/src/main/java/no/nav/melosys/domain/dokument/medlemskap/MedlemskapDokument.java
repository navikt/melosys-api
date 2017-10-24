package no.nav.melosys.domain.dokument.medlemskap;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MedlemskapDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name="medlemsperiode")
    @XmlElement(name="medlemsperiode")
    private List<Medlemsperiode> medlemsperiode;

    public List<Medlemsperiode> getMedlemsperiode() {
        return medlemsperiode;
    }

    public void setMedlemsperiode(List<Medlemsperiode> medlemsperiode) {
        this.medlemsperiode = medlemsperiode;
    }
}
