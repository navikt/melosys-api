package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.penger.Penger;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import org.hibernate.annotations.Columns;
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
    @Type(type = "no.nav.melosys.domain.avgift.penger.TrygdeavgiftsbeloepMndType")
    private Penger trygdeavgiftsbeløpMd;

    @Column(name = "trygdesats", nullable = false)
    private BigDecimal trygdesats;

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

    public Medlemskapsperiode hentGjeldendeMedlemskapsperiode() {
        var gjeldendeMedlemskapsperioder = fastsattTrygdeavgift.getMedlemAvFolketrygden().getMedlemskapsperioder()
            .stream()
            .filter(medlemskapsperiode ->
                (medlemskapsperiode.getFom().equals(periodeFra) || medlemskapsperiode.getFom().isBefore(periodeFra))
                    && (medlemskapsperiode.getTom().isEqual(periodeTil) || medlemskapsperiode.getTom().isAfter(periodeTil)))
            .toList();
        if (gjeldendeMedlemskapsperioder.size() != 1) {
            throw new FunksjonellException("Finner " + gjeldendeMedlemskapsperioder.size() + " gjeldende medlemskapsperioder. Forventet én.");
        }
        return gjeldendeMedlemskapsperioder.get(0);
    }

    public Trygdedekninger hentGjeldendeTrygdedekning() {
        return hentGjeldendeMedlemskapsperiode().getTrygdedekning();
    }

    public Inntektsperiode hentGjeldendeInntektsperiode() {
        var getGjeldendeInntektsperiode = fastsattTrygdeavgift.getTrygdeavgiftsgrunnlag().getInntektsperioder()
            .stream()
            .filter(inntektsperiode ->
                (inntektsperiode.getFomDato().equals(periodeFra) || inntektsperiode.getFomDato().isBefore(periodeFra))
                    && (inntektsperiode.getTomDato().isEqual(periodeTil) || inntektsperiode.getTomDato().isAfter(periodeTil)))
            .filter(inntektsperiode -> !inntektsperiode.isTrygdeavgiftBetalesTilSkatt())
            .toList();
        if (getGjeldendeInntektsperiode.size() != 1) {
            throw new FunksjonellException("Finner " + getGjeldendeInntektsperiode.size() + " gjeldende medlemskapsperioder. Forventet én.");
        }
        return getGjeldendeInntektsperiode.get(0);
    }

    public BigDecimal hentGjeldendeAvgiftspliktigInntekt() {
        return hentGjeldendeInntektsperiode().getAvgiftspliktigInntektMnd().getVerdi();
    }
}
