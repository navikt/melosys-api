package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;

import javax.xml.bind.annotation.*;
import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "hentPeriodeListeResponse",
        propOrder = {"periodeListe"}
)
@XmlSeeAlso(Medlemsperiode.class)
public class HentPeriodeListeResponseWrapper {

    @XmlElementWrapper(name = "response")
    private List<Medlemsperiode> periodeListe = new ArrayList<>();

    public HentPeriodeListeResponseWrapper() {
    }

    public HentPeriodeListeResponseWrapper withPeriodeListe(Collection<Medlemsperiode> periodeListe) {
        if (periodeListe != null)
            this.periodeListe.addAll(periodeListe);
        return this;
    }
}
