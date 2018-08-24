package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Arbeidsavtale implements HarPeriode {

    public Arbeidstidsordning arbeidstidsordning; //"http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger"

    public String avloenningstype; //"http://nav.no/kodeverk/Kodeverk/Avl_c3_b8nningstyper"

    public Yrke yrke; //"http://nav.no/kodeverk/Kodeverk/Yrker"

    public Periode gyldighetsperiode;

    public BigDecimal avtaltArbeidstimerPerUke;

    public BigDecimal stillingsprosent;

    @JsonIgnore
    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate sisteLoennsendringsdato;

    public BigDecimal beregnetAntallTimerPrUke;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    public LocalDate endringsdatoStillingsprosent;

    @JsonProperty("fartsomraade")
    public String fartsområde; //"http://nav.no/kodeverk/Kodeverk/Fartsomr_c3_a5der"

    public String skipsregister; //"http://nav.no/kodeverk/Kodeverk/Skipsregistre"

    public String skipstype; //"http://nav.no/kodeverk/Kodeverk/Skipstyper"

    public Boolean maritimArbeidsavtale;

    public BigDecimal beregnetStillingsprosent;

    public BigDecimal antallTimerGammeltAa;

    public Arbeidstidsordning getArbeidstidsordning() {
        return arbeidstidsordning;
    }

    public String getAvloenningstype() {
        return avloenningstype;
    }

    public Yrke getYrke() {
        return yrke;
    }

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return gyldighetsperiode;
    }

    public BigDecimal getAvtaltArbeidstimerPerUke() {
        return avtaltArbeidstimerPerUke;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public LocalDate getSisteLoennsendringsdato() {
        return sisteLoennsendringsdato;
    }

    public BigDecimal getBeregnetAntallTimerPrUke() {
        return beregnetAntallTimerPrUke;
    }

    public LocalDate getEndringsdatoStillingsprosent() {
        return endringsdatoStillingsprosent;
    }

    public String getFartsområde() {
        return fartsområde;
    }

    public String getSkipsregister() {
        return skipsregister;
    }

    public String getSkipstype() {
        return skipstype;
    }

    public Boolean getMaritimArbeidsavtale() {
        return maritimArbeidsavtale;
    }

    public BigDecimal getBeregnetStillingsprosent() {
        return beregnetStillingsprosent;
    }

    public BigDecimal getAntallTimerGammeltAa() {
        return antallTimerGammeltAa;
    }

}