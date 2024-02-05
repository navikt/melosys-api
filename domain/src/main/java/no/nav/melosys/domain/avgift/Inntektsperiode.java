package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Inntektskildetype;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

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

    @Columns(columns = {
        @Column(name = "avgiftspliktig_inntekt_mnd_verdi"),
        @Column(name = "avgiftspliktig_inntekt_mnd_valuta")})
    @Type(type = "no.nav.melosys.domain.avgift.PengerType", parameters = {
        @Parameter(name = "verdiPropertyName", value = "avgiftspliktig_inntekt_mnd_verdi"),
        @Parameter(name = "valutaPropertyName", value = "avgiftspliktig_inntekt_mnd_valuta")})
    private Penger avgiftspliktigInntektMnd;

    @Column(name = "aga_betales_til_skatt")
    private boolean arbeidsgiversavgiftBetalesTilSkatt;

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

    public Penger getAvgiftspliktigInntektMnd() {
        return avgiftspliktigInntektMnd;
    }

    public void setAvgiftspliktigInntektMnd(Penger avgiftspliktigInntektMnd) {
        this.avgiftspliktigInntektMnd = avgiftspliktigInntektMnd;
    }

    public boolean isArbeidsgiversavgiftBetalesTilSkatt() {
        return arbeidsgiversavgiftBetalesTilSkatt;
    }

    public void setArbeidsgiversavgiftBetalesTilSkatt(boolean arbeidsgiversavgiftBetalesTilSkatt) {
        this.arbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetalesTilSkatt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inntektsperiode that = (Inntektsperiode) o;
        return Objects.equals(trygdeavgiftsgrunnlag, that.trygdeavgiftsgrunnlag)
            && Objects.equals(fomDato, that.fomDato) && Objects.equals(tomDato, that.tomDato)
            && Objects.equals(type, that.type)
            && Objects.equals(avgiftspliktigInntektMnd, that.avgiftspliktigInntektMnd)
            && arbeidsgiversavgiftBetalesTilSkatt == that.arbeidsgiversavgiftBetalesTilSkatt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trygdeavgiftsgrunnlag, fomDato, tomDato, type, avgiftspliktigInntektMnd,
            arbeidsgiversavgiftBetalesTilSkatt);
    }

    @Override
    public String toString() {
        return "Inntektsperiode{" + "id=" + id + ", fomDato=" + fomDato + ", tomDato=" + tomDato + ", type=" + type
            + ", avgiftspliktigInntektMnd=" + avgiftspliktigInntektMnd + ", arbeidsgiversavgiftBetalesTilSkatt="
            + arbeidsgiversavgiftBetalesTilSkatt + '}';
    }
}
