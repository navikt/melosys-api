package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Arbeidsavtale {

    private String arbeidstidsordning; //"http://nav.no/kodeverk/Kodeverk/Arbeidstidsordninger"

    private String avloenningstype; //"http://nav.no/kodeverk/Kodeverk/Avl_c3_b8nningstyper"

    private String yrke; //"http://nav.no/kodeverk/Kodeverk/Yrker"

    private BigDecimal avtaltArbeidstimerPerUke;

    private BigDecimal stillingsprosent;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate sisteLoennsendringsdato;

    private BigDecimal beregnetAntallTimerPrUke;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate endringsdatoStillingsprosent;

    @JsonProperty("fartsomraade")
    private String fartsområde; //"http://nav.no/kodeverk/Kodeverk/Fartsomr_c3_a5der"

    private String skipsregister; //"http://nav.no/kodeverk/Kodeverk/Skipsregistre"

    private String skipstype; //"http://nav.no/kodeverk/Kodeverk/Skipstyper"

    private Boolean maritimArbeidsavtale;

    public String getArbeidstidsordning() {
        return arbeidstidsordning;
    }

    public void setArbeidstidsordning(String arbeidstidsordning) {
        this.arbeidstidsordning = arbeidstidsordning;
    }

    public String getAvloenningstype() {
        return avloenningstype;
    }

    public void setAvloenningstype(String avloenningstype) {
        this.avloenningstype = avloenningstype;
    }

    public String getYrke() {
        return yrke;
    }

    public void setYrke(String yrke) {
        this.yrke = yrke;
    }

    public BigDecimal getAvtaltArbeidstimerPerUke() {
        return avtaltArbeidstimerPerUke;
    }

    public void setAvtaltArbeidstimerPerUke(BigDecimal avtaltArbeidstimerPerUke) {
        this.avtaltArbeidstimerPerUke = avtaltArbeidstimerPerUke;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public LocalDate getSisteLoennsendringsdato() {
        return sisteLoennsendringsdato;
    }

    public void setSisteLoennsendringsdato(LocalDate sisteLoennsendringsdato) {
        this.sisteLoennsendringsdato = sisteLoennsendringsdato;
    }

    public BigDecimal getBeregnetAntallTimerPrUke() {
        return beregnetAntallTimerPrUke;
    }

    public void setBeregnetAntallTimerPrUke(BigDecimal beregnetAntallTimerPrUke) {
        this.beregnetAntallTimerPrUke = beregnetAntallTimerPrUke;
    }

    public LocalDate getEndringsdatoStillingsprosent() {
        return endringsdatoStillingsprosent;
    }

    public void setEndringsdatoStillingsprosent(LocalDate endringsdatoStillingsprosent) {
        this.endringsdatoStillingsprosent = endringsdatoStillingsprosent;
    }

    public String getFartsområde() {
        return fartsområde;
    }

    public void setFartsområde(String fartsområde) {
        this.fartsområde = fartsområde;
    }

    public String getSkipsregister() {
        return skipsregister;
    }

    public void setSkipsregister(String skipsregister) {
        this.skipsregister = skipsregister;
    }

    public String getSkipstype() {
        return skipstype;
    }

    public void setSkipstype(String skipstype) {
        this.skipstype = skipstype;
    }

    public Boolean getMaritimArbeidsavtale() {
        return maritimArbeidsavtale;
    }

    public void setMaritimArbeidsavtale(Boolean maritimArbeidsavtale) {
        this.maritimArbeidsavtale = maritimArbeidsavtale;
    }

}
