package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import javax.persistence.*;

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;

@Entity
@Table(name = "trygdeavgiftt")
public class Trygdeavgift {

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

    @Column(name = "trygdeavgift_beloep_md", nullable = false)
    private BigInteger trygdeavgiftsbeløpMd;

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

    public BigInteger getTrygdeavgiftsbeløpMd() {
        return trygdeavgiftsbeløpMd;
    }

    public void setTrygdeavgiftsbeløpMd(BigInteger trygdeavgiftsbeløpMd) {
        this.trygdeavgiftsbeløpMd = trygdeavgiftsbeløpMd;
    }

    public BigDecimal getTrygdesats() {
        return trygdesats;
    }

    public void setTrygdesats(BigDecimal trygdesats) {
        this.trygdesats = trygdesats;
    }
}
