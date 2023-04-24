package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trygdeavgiftsgrunnlag")
public class Trygdeavgiftsgrunnlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "fastsatt_trygdeavgift_id", nullable = false, updatable = false)
    private FastsattTrygdeavgift fastsattTrygdeavgift;

    @OneToMany(mappedBy = "trygdeavgiftsgrunnlag", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<SkatteforholdTilNorge> skatteforholdTilNorge = new HashSet<>(1);

    @OneToMany(mappedBy = "trygdeavgiftsgrunnlag", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Inntektsperiode> inntektsperioder = new HashSet<>(1);

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

    public void setSkatteforholdTilNorge(Set<SkatteforholdTilNorge> skatteforholdTilNorge) {
        this.skatteforholdTilNorge.clear();
        skatteforholdTilNorge.forEach(forhold -> forhold.setTrygdeavgiftsgrunnlag(this));
        this.skatteforholdTilNorge.addAll(skatteforholdTilNorge);
    }

    public Set<Inntektsperiode> getInntektsperioder() {
        return inntektsperioder;
    }

    public void setInntektsperioder(Set<Inntektsperiode> inntektsperioder) {
        this.inntektsperioder.clear();
        inntektsperioder.forEach(periode -> periode.setTrygdeavgiftsgrunnlag(this));
        this.inntektsperioder.addAll(inntektsperioder);
    }
}
