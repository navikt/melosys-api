package no.nav.melosys.integrasjon.medl.medlemskap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.annotation.*;

import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;

/**
 * Denne klassen har som formål å sørge for at respons fra MEDL marshalles til samme format som vi får fra tjenesten.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "hentPeriodeListeResponse",
        propOrder = {"periodeListe"}
)
@XmlSeeAlso(Medlemsperiode.class)
public class HentPeriodeListeResponseWrapper {

    @XmlElementWrapper(name = "response")
    private List<Medlemsperiode> periodeListe = new ArrayList<>();

    public HentPeriodeListeResponseWrapper withPeriodeListe(Collection<Medlemsperiode> periodeListe) {
        this.periodeListe.addAll(periodeListe);
        return this;
    }
}
