package no.nav.melosys.domain.avgift;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.kodeverk.Inntektskildetype;

@Entity
@Table(name = "inntektsperiode")
public class Inntektsperiode implements ErPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "grunnlagInntekstperiode")
    private Set<Trygdeavgiftsperiode> trygdeavgiftsperioder;

    @Column(name = "fom_dato", nullable = false)
    private LocalDate fomDato;

    @Column(name = "tom_dato", nullable = false)
    private LocalDate tomDato;

    @Column(name = "inntektskilde_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Inntektskildetype type;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "avgiftspliktig_inntekt_mnd_verdi"))
    @AttributeOverride(name = "valuta", column = @Column(name = "avgiftspliktig_inntekt_mnd_valuta"))
    private Penger avgiftspliktigInntekt;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "avgiftspliktig_inntekt_total_verdi"))
    @AttributeOverride(name = "valuta", column = @Column(name = "avgiftspliktig_inntekt_total_valuta"))
    private Penger avgiftspliktigTotalInntekt;

    @Column(name = "aga_betales_til_skatt")
    private boolean arbeidsgiversavgiftBetalesTilSkatt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Penger getAvgiftspliktigInntekt() {
        return avgiftspliktigInntekt;
    }

    public void setAvgiftspliktigInntekt(Penger avgiftspliktigInntekt) {
        this.avgiftspliktigInntekt = avgiftspliktigInntekt;
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
        return arbeidsgiversavgiftBetalesTilSkatt == that.arbeidsgiversavgiftBetalesTilSkatt && Objects.equals(id, that.id) && Objects.equals(fomDato, that.fomDato) && Objects.equals(tomDato, that.tomDato) && type == that.type && Objects.equals(avgiftspliktigTotalInntekt, that.avgiftspliktigTotalInntekt) &&
            Objects.equals(avgiftspliktigInntekt, that.avgiftspliktigInntekt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fomDato, tomDato, type, avgiftspliktigInntekt, arbeidsgiversavgiftBetalesTilSkatt, avgiftspliktigTotalInntekt);
    }

    @Override
    public String toString() {
        return "Inntektsperiode{" + "id=" + id + ", fomDato=" + fomDato
            + ", tomDato=" + tomDato + ", type=" + type
            + ", avgiftspliktigInntektMnd=" + avgiftspliktigInntekt
            + ", avgiftspliktigTotalInntekt=" + avgiftspliktigTotalInntekt
            + ", arbeidsgiversavgiftBetalesTilSkatt=" + arbeidsgiversavgiftBetalesTilSkatt + '}';
    }

    public Set<Trygdeavgiftsperiode> getTrygdeavgiftsperioder() {
        return trygdeavgiftsperioder;
    }

    @Override
    public LocalDate getFom() {
        return getFomDato();
    }

    @Override
    public LocalDate getTom() {
        return getTomDato();
    }

    public boolean isErMaanedsbelop() {
        return avgiftspliktigInntekt != null;
    }

    public Penger getAvgiftspliktigTotalInntekt() {
        return avgiftspliktigTotalInntekt;
    }

    public void setAvgiftspliktigTotalInntekt(Penger avgiftspliktigTotalInntekt) {
        this.avgiftspliktigTotalInntekt = avgiftspliktigTotalInntekt;
    }
}
