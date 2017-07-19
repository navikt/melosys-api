package no.nav.melosys.domain;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "ARBEIDSAVTALE")
public class Arbeidsavtale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "arbeidsforhold_id")
    private Arbeidsforhold arbeidsforhold;

    @Column(name = "fartsomraade")
    private String fartsområde;

    @Column(name = "skipsregister")
    private String skipsregister;

    @Column(name = "skipstype")
    private String skipstype;

    @Column(name = "stillingsprosent")
    private BigDecimal stillingsprosent;

    @Column(name = "timer_per_uke")
    private BigDecimal timerPerUke;

    @Column(name = "yrke")
    private String yrke;

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
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

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public BigDecimal getTimerPerUke() {
        return timerPerUke;
    }

    public void setTimerPerUke(BigDecimal timerPerUke) {
        this.timerPerUke = timerPerUke;
    }

    public String getYrke() {
        return yrke;
    }

    public void setYrke(String yrke) {
        this.yrke = yrke;
    }
}
