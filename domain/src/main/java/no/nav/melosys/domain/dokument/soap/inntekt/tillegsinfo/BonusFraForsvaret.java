package no.nav.melosys.domain.dokument.soap.inntekt.tillegsinfo;

import java.time.Year;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.jaxb.XMLYearToYearAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BonusFraForsvaret")
public class BonusFraForsvaret extends TilleggsinformasjonDetaljer {

    @XmlElement(name = "aaretUtbetalingenGjelderFor")
    @XmlJavaTypeAdapter(XMLYearToYearAdapter.class)
    public Year åretUtbetalingenGjelderFor;
}
