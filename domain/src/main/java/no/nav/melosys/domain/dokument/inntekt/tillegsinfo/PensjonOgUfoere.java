package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.inntekt.Periode;

public class PensjonOgUfoere extends TilleggsinformasjonDetaljer implements HarPeriode {

    @JsonProperty("grunnpensjonbeloep")
    @XmlElement(name = "grunnpensjonbeloep")
    public BigDecimal grunnpensjonbeløp;

    public BigDecimal heravEtterlattepensjon;

    public Integer pensjonsgrad;

    public Periode tidsrom;

    @JsonProperty("tillegspensjonbeloep")
    public BigDecimal tillegspensjonbeløp;

    @JsonProperty("ufoeregradpensjonsgrad")
    public Integer uføreEllerPensjonsgrad;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return tidsrom;
    }
}
