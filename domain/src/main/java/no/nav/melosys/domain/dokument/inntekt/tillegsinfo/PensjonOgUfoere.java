package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AldersUfoereEtterlatteAvtalefestetOgKrigspensjon")
public class PensjonOgUfoere extends TilleggsinformasjonDetaljer implements HarPeriode {

    @JsonProperty("grunnpensjonbeloep")
    @XmlElement(name = "grunnpensjonbeloep")
    public BigDecimal grunnpensjonbeløp;

    public BigDecimal heravEtterlattepensjon;

    public Integer pensjonsgrad;

    public Periode tidsrom;

    @JsonProperty("tillegspensjonbeloep")
    @XmlElement(name = "tillegspensjonbeloep")
    public BigDecimal tillegspensjonbeløp;

    @JsonProperty("ufoeregradpensjonsgrad")
    @XmlElement(name = "ufoeregradpensjonsgrad")
    public Integer uføreEllerPensjonsgrad;

    @Override
    public ErPeriode getPeriode() {
        return tidsrom;
    }
}
