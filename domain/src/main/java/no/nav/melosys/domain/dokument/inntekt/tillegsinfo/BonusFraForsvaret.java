package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import no.nav.melosys.domain.dokument.jaxb.XMLYearToYearAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Year;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BonusFraForsvaret")
public class BonusFraForsvaret extends TilleggsinformasjonDetaljer {

    @XmlJavaTypeAdapter(XMLYearToYearAdapter.class)
    public Year aaretUtbetalingenGjelderFor;
}
