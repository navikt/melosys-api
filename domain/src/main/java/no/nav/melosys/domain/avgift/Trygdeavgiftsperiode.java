package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.*;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;

@Entity
@Table(name = "trygdeavgiftsperiode")
public class Trygdeavgiftsperiode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "fastsatt_trygdeavgift_id")
    private FastsattTrygdeavgift fastsattTrygdeavgift;

    @Column(name = "periode_fra", nullable = false)
    private LocalDate periodeFra;

    @Column(name = "periode_til", nullable = false)
    private LocalDate periodeTil;

    @Embedded
    private Penger trygdeavgiftsbeløpMd;

    @Column(name = "trygdesats", nullable = false)
    private BigDecimal trygdesats;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "inntektsperiode_id")
    private Inntektsperiode grunnlagInntekstperiode;

    @ManyToOne
    @JoinColumn(name = "medlemskapsperiode_id")
    private Medlemskapsperiode grunnlagMedlemskapsperiode;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "skatteforhold_id")
    private SkatteforholdTilNorge grunnlagSkatteforholdTilNorge;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FastsattTrygdeavgift getFastsattTrygdeavgift() {
        return fastsattTrygdeavgift;
    }

    public void setFastsattTrygdeavgift(FastsattTrygdeavgift fastsattTrygdeavgift) {
        this.fastsattTrygdeavgift = fastsattTrygdeavgift;
    }

    public LocalDate getPeriodeFra() {
        return periodeFra;
    }

    public void setPeriodeFra(LocalDate periodeFra) {
        this.periodeFra = periodeFra;
    }

    public LocalDate getPeriodeTil() {
        return periodeTil;
    }

    public void setPeriodeTil(LocalDate periodeTil) {
        this.periodeTil = periodeTil;
    }

    public Penger getTrygdeavgiftsbeløpMd() {
        return trygdeavgiftsbeløpMd;
    }

    public void setTrygdeavgiftsbeløpMd(Penger trygdeavgiftsbeløpMd) {
        this.trygdeavgiftsbeløpMd = trygdeavgiftsbeløpMd;
    }

    public BigDecimal getTrygdesats() {
        return trygdesats;
    }

    public void setTrygdesats(BigDecimal trygdesats) {
        this.trygdesats = trygdesats;
    }

    public Inntektsperiode getGrunnlagInntekstperiode() {
        return grunnlagInntekstperiode;
    }

    public void setGrunnlagInntekstperiode(Inntektsperiode grunnlagInntekstperiode) {
        this.grunnlagInntekstperiode = grunnlagInntekstperiode;
    }

    public Medlemskapsperiode getGrunnlagMedlemskapsperiode() {
        return grunnlagMedlemskapsperiode;
    }

    public void setGrunnlagMedlemskapsperiode(Medlemskapsperiode grunnlagMedlemskapsperiode) {
        this.grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode;
    }

    public SkatteforholdTilNorge getGrunnlagSkatteforholdTilNorge() {
        return grunnlagSkatteforholdTilNorge;
    }

    public void setGrunnlagSkatteforholdTilNorge(SkatteforholdTilNorge grunnlagSkatteforholdTilNorge) {
        this.grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge;
    }

    public boolean harAvgift() {
        return BigDecimal.ZERO.compareTo(this.trygdesats) != 0 && BigDecimal.ZERO.compareTo(this.trygdeavgiftsbeløpMd.getVerdi()) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trygdeavgiftsperiode that = (Trygdeavgiftsperiode) o;
        return Objects.equals(id, that.id) && Objects.equals(fastsattTrygdeavgift, that.fastsattTrygdeavgift) && Objects.equals(periodeFra, that.periodeFra) && Objects.equals(periodeTil, that.periodeTil) && Objects.equals(trygdeavgiftsbeløpMd, that.trygdeavgiftsbeløpMd) && Objects.equals(trygdesats, that.trygdesats) && Objects.equals(grunnlagInntekstperiode, that.grunnlagInntekstperiode) && Objects.equals(grunnlagMedlemskapsperiode, that.grunnlagMedlemskapsperiode) && Objects.equals(grunnlagSkatteforholdTilNorge, that.grunnlagSkatteforholdTilNorge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fastsattTrygdeavgift, periodeFra, periodeTil, trygdeavgiftsbeløpMd, trygdesats, grunnlagInntekstperiode, grunnlagMedlemskapsperiode, grunnlagSkatteforholdTilNorge);
    }
}
