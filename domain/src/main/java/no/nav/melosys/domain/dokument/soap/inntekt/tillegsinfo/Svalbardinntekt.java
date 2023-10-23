package no.nav.melosys.domain.dokument.soap.inntekt.tillegsinfo;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Svalbardinntekt")
public class Svalbardinntekt extends TilleggsinformasjonDetaljer {

    public Integer antallDager;

    public BigDecimal betaltTrygdeavgift;
}
