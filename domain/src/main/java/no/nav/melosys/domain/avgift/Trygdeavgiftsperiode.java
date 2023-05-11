package no.nav.melosys.domain.avgift;

import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

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

    @Columns(columns = {
        @Column(name = "trygdeavgift_beloep_mnd_verdi", nullable = false),
        @Column(name = "trygdeavgift_beloep_mnd_valuta", nullable = false)})
    @Type(type = "no.nav.melosys.domain.avgift.PengerType", parameters = {
        @Parameter(name = "verdiPropertyName", value = "trygdeavgift_beloep_mnd_verdi"),
        @Parameter(name = "valutaPropertyName", value = "trygdeavgift_beloep_mnd_valuta")})
    private Penger trygdeavgiftsbeløpMd;

    @Column(name = "trygdesats", nullable = false)
    private Double trygdesats;

    @OneToOne
    @JoinColumn(name = "inntektsperiode_id")
    private Inntektsperiode grunnlagInntekstperiode;

    @OneToOne
    @JoinColumn(name = "medlemskapsperiode_id")
    private Medlemskapsperiode grunnlagMedlemskapsperiode;

    @OneToOne
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

    public Double getTrygdesats() {
        return trygdesats;
    }

    public void setTrygdesats(Double trygdesats) {
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
}
