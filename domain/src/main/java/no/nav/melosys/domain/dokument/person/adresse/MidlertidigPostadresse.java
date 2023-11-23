package no.nav.melosys.domain.dokument.person.adresse;

import java.time.LocalDateTime;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MidlertidigPostadresse")
@XmlSeeAlso({MidlertidigPostadresseNorge.class, MidlertidigPostadresseUtland.class})
public class MidlertidigPostadresse {

    public LocalDateTime endringstidspunkt;

    public Land land;
    public Periode postleveringsPeriode;

}
