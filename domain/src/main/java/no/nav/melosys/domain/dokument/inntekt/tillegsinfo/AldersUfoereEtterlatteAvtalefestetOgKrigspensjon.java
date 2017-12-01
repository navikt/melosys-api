package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import no.nav.melosys.domain.dokument.felles.Periode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AldersUfoereEtterlatteAvtalefestetOgKrigspensjon")
public class AldersUfoereEtterlatteAvtalefestetOgKrigspensjon extends TilleggsinformasjonDetaljer {

    private Periode tidsrom;

    public Periode getTidsrom() {
        return tidsrom;
    }

    public void setTidsrom(Periode tidsrom) {
        this.tidsrom = tidsrom;
    }
}
