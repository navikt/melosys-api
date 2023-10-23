package no.nav.melosys.domain.dokument.soap.inntekt.tillegsinfo;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReiseKostOgLosji")
public class ReiseKostOgLosji extends TilleggsinformasjonDetaljer {

    public String persontype; //"http://nav.no/kodeverk/Kodeverk/PersontypeForReiseKostLosji"
}
