package no.nav.melosys.domain.avgift;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
    @Fetch(value = FetchMode.SUBSELECT)
    private List<SkatteforholdTilNorge> skatteforholdTilNorge = new ArrayList<>(1);

    @OneToMany(mappedBy = "trygdeavgiftsgrunnlag", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Inntektsperiode> inntektsperioder = new ArrayList<>(1);

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

    public List<SkatteforholdTilNorge> getSkatteforholdTilNorge() {
        return skatteforholdTilNorge;
    }

    public void setSkatteforholdTilNorge(List<SkatteforholdTilNorge> skatteforholdTilNorge) {
        this.skatteforholdTilNorge.clear();
        skatteforholdTilNorge.forEach(forhold -> forhold.setTrygdeavgiftsgrunnlag(this));
        this.skatteforholdTilNorge.addAll(skatteforholdTilNorge);
    }

    public List<Inntektsperiode> getInntektsperioder() {
        return inntektsperioder;
    }

    public void setInntektsperioder(List<Inntektsperiode> inntektsperioder) {
        this.inntektsperioder.clear();
        inntektsperioder.forEach(periode -> periode.setTrygdeavgiftsgrunnlag(this));
        this.inntektsperioder.addAll(inntektsperioder);
    }
}
