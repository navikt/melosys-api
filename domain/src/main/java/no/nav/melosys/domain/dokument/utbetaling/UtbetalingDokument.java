package no.nav.melosys.domain.dokument.utbetaling;

import java.util.List;
import javax.xml.bind.annotation.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UtbetalingDokument implements SaksopplysningDokument {

    @XmlElementWrapper(name = "utbetalinger")
    @XmlElement(name = "utbetaling")
    public List<Utbetaling> utbetalinger;
}
