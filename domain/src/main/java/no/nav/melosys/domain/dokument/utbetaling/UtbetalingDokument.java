package no.nav.melosys.domain.dokument.utbetaling;

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.*;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UtbetalingDokument extends SaksopplysningDokument {

    @XmlElementWrapper(name = "utbetalinger")
    @XmlElement(name = "utbetaling")
    public List<Utbetaling> utbetalinger;

    public List<Ytelse> hentBarnetrygdytelser() {
        return utbetalinger.stream()
            .flatMap(utbetaling -> utbetaling.ytelser.stream())
            .filter(ytelse -> "Barnetrygd".equals(ytelse.type))
            .collect(Collectors.toList());
    }
}
