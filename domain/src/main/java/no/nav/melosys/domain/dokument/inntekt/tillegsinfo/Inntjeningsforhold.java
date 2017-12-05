package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Inntjeningsforhold")
public class Inntjeningsforhold extends TilleggsinformasjonDetaljer {

    public String inntjeningsforhold; //"http://nav.no/kodeverk/Kodeverk/SpesielleInntjeningsforhold"
}
