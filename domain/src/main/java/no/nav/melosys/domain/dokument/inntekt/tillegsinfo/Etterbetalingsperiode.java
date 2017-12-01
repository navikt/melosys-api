package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import no.nav.melosys.domain.dokument.felles.Periode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Etterbetalingsperiode")
public class Etterbetalingsperiode extends TilleggsinformasjonDetaljer {

    private Periode etterbetalingsperiode;

    public Periode getEtterbetalingsperiode() {
        return etterbetalingsperiode;
    }

    public void setEtterbetalingsperiode(Periode etterbetalingsperiode) {
        this.etterbetalingsperiode = etterbetalingsperiode;
    }
}
