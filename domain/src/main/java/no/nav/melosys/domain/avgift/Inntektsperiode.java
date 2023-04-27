package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Inntektskildetype;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "inntektsperiode")
public class Inntektsperiode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trygdeavgiftsgrunnlag_id", nullable = false, updatable = false)
    private Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag;

    @Column(name = "fom_dato", nullable = false)
    private LocalDate fomDato;

    @Column(name = "tom_dato", nullable = false)
    private LocalDate tomDato;

    @Column(name = "inntektskilde_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Inntektskildetype type;

    @Column(name = "avgiftspliktig_inntekt_mnd")
    private BigInteger avgiftspliktigInntektMnd;

    @Column(name = "aga_betales_til_skatt")
    private boolean arbeidsgiversavgiftBetalesTilSkatt;

    @Column(name = "trygdeavgift_betales_til_skatt")
    private boolean trygdeavgiftBetalesTilSkatt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trygdeavgiftsgrunnlag getTrygdeavgiftsgrunnlag() {
        return trygdeavgiftsgrunnlag;
    }

    public void setTrygdeavgiftsgrunnlag(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag) {
        this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag;
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

    public Inntektskildetype getType() {
        return type;
    }

    public void setType(Inntektskildetype inntektskildetype) {
        this.type = inntektskildetype;
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

    public boolean utledTrygdeavgiftBetalesTilSkatt(Skatteplikttype skatteplikttype) {
        return (skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG) ||
            List.of(Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE, Inntektskildetype.FN_SKATTEFRITAK).contains(type) ||
            (type == Inntektskildetype.INNTEKT_FRA_UTLANDET && arbeidsgiversavgiftBetalesTilSkatt);
    }
}
