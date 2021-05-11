package no.nav.melosys.domain.dokument.person.adresse;

import java.time.LocalDateTime;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateTimeXmlAdapter;

public class BostedsadressePeriode implements HarPeriode {

    public Periode periode;

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    public LocalDateTime endringstidspunkt;

    public Bostedsadresse bostedsadresse;

    @Override
    public ErPeriode getPeriode() {
        return periode;
    }
}
