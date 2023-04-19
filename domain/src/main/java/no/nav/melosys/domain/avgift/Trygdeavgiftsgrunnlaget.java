package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trygdeavgiftsgrunnlaget")
public class Trygdeavgiftsgrunnlaget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "fastsatt_trygdeavgift_id", nullable = false, updatable = false)
    private FastsattTrygdeavgift fastsattTrygdeavgift;

    @OneToMany(mappedBy = "trygdeavgiftsgrunnlaget", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<SkatteforholdTilNorge> skatteforholdTilNorge = new HashSet<>(1);

    @OneToMany(mappedBy = "trygdeavgiftsgrunnlaget", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Inntektskilde> inntektskilder = new HashSet<>(1);

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

    public Set<SkatteforholdTilNorge> getSkatteforholdTilNorge() {
        return skatteforholdTilNorge;
    }

    public void setSkatteforholdTilNorge(Set<SkatteforholdTilNorge> skattefoholdTilNorge) {
        this.skatteforholdTilNorge = skattefoholdTilNorge;
    }

    public Set<Inntektskilde> getInntektskilder() {
        return inntektskilder;
    }

    public void setInntektskilder(Set<Inntektskilde> inntektskilder) {
        this.inntektskilder = inntektskilder;
    }
}
