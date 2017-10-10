package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

// FIXME Ikke implementert i xslt
@XmlAccessorType(XmlAccessType.FIELD)
public class Tilleggsinformasjon {

    @XmlElement(required = true)
    private String kategori;

    private TilleggsinformasjonDetaljer tilleggsinformasjonDetaljer;

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public TilleggsinformasjonDetaljer getTilleggsinformasjonDetaljer() {
        return tilleggsinformasjonDetaljer;
    }

    public void setTilleggsinformasjonDetaljer(TilleggsinformasjonDetaljer tilleggsinformasjonDetaljer) {
        this.tilleggsinformasjonDetaljer = tilleggsinformasjonDetaljer;
    }
}
