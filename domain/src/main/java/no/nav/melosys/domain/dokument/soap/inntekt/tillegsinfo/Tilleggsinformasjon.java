package no.nav.melosys.domain.dokument.soap.inntekt.tillegsinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Tilleggsinformasjon {

    @XmlElement(required = true)
    public String kategori;

    public TilleggsinformasjonDetaljer tilleggsinformasjonDetaljer;
}
