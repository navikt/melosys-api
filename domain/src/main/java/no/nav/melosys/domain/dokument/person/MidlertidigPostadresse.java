package no.nav.melosys.domain.dokument.person;

import java.time.LocalDateTime;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateTimeXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MidlertidigPostadresse")
@XmlSeeAlso({MidlertidigPostadresseNorge.class, MidlertidigPostadresseUtland.class})
public class MidlertidigPostadresse {

    @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
    public LocalDateTime endringstidspunkt;

    public Land land;
    public Periode postleveringsPeriode;

}
