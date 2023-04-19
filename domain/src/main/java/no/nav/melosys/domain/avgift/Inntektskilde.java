package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Inntektskildetype;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.LocalDate;

@Entity
@Table(name = "inntektskilde")
public class Inntektskilde {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trygdeavgiftsgrunnlaget_id", nullable = false, updatable = false)
    private Trygdeavgiftsgrunnlaget trygdeavgiftsgrunnlaget;

    @Column(name = "fom_dato", nullable = false)
    private LocalDate fomDato;

    @Column(name = "tom_dato", nullable = false)
    private LocalDate tomDato;

    @Column(name = "inntektskilde_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Inntektskildetype inntektskildetype;

    @Column(name = "avgiftspliktig_inntekt_mnd")
    private BigInteger avgiftspliktigInntektMnd;

    @Column(name = "arbeidsgiversavgift_betales_til_skatt")
    private boolean arbeidsgiversavgiftBetalesTilSkatt;

    @Column(name = "trygdeavgift_betales_til_skatt")
    private boolean trygdeavgiftBetalesTilSkatt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trygdeavgiftsgrunnlaget getTrygdeavgiftsgrunnlaget() {
        return trygdeavgiftsgrunnlaget;
    }

    public void setTrygdeavgiftsgrunnlaget(Trygdeavgiftsgrunnlaget trygdeavgiftsgrunnlaget) {
        this.trygdeavgiftsgrunnlaget = trygdeavgiftsgrunnlaget;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public Inntektskildetype getInntektskildetype() {
        return inntektskildetype;
    }

    public void setInntektskildetype(Inntektskildetype inntektskildetype) {
        this.inntektskildetype = inntektskildetype;
    }

    public BigInteger getAvgiftspliktigInntektMnd() {
        return avgiftspliktigInntektMnd;
    }

    public void setAvgiftspliktigInntektMnd(BigInteger avgiftspliktigInntektMnd) {
        this.avgiftspliktigInntektMnd = avgiftspliktigInntektMnd;
    }

    public boolean isArbeidsgiversavgiftBetalesTilSkatt() {
        return arbeidsgiversavgiftBetalesTilSkatt;
    }

    public void setArbeidsgiversavgiftBetalesTilSkatt(boolean arbeidsgiversavgiftBetalesTilSkatt) {
        this.arbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetalesTilSkatt;
    }

    public boolean isTrygdeavgiftBetalesTilSkatt() {
        return trygdeavgiftBetalesTilSkatt;
    }

    public void setTrygdeavgiftBetalesTilSkatt(boolean trygdeavgiftBetalesTilSkatt) {
        this.trygdeavgiftBetalesTilSkatt = trygdeavgiftBetalesTilSkatt;
    }
}
