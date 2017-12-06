package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

import javax.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AldersUfoereEtterlatteAvtalefestetOgKrigspensjon")
public class PensjonOgUfoere extends TilleggsinformasjonDetaljer implements HarPeriode {

    @XmlElement(name = "grunnpensjonbeloep")
    public BigDecimal grunnpensjonbeløp;

    public BigDecimal heravEtterlattepensjon;

    public Integer pensjonsgrad;

    public Periode tidsrom;

    @XmlElement(name = "tillegspensjonbeloep")
    public BigDecimal tillegspensjonbeløp;

    @XmlElement(name = "ufoeregradpensjonsgrad")
    public Integer uføreEllerPensjonsgrad;

    @Override
    public ErPeriode getPeriode() {
        return tidsrom;
    }
}
