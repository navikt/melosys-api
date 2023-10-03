package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

public class Svalbardinntekt extends TilleggsinformasjonDetaljer {

    public Integer antallDager;

    public BigDecimal betaltTrygdeavgift;
}
