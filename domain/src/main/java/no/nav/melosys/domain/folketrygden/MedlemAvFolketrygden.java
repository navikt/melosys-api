package no.nav.melosys.domain.folketrygden;

import java.util.Collection;
import java.util.HashSet;
import javax.persistence.*;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt;

@Entity
@Table(name = "medlem_av_folketrygden")
public class MedlemAvFolketrygden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Column(name = "trygdeavgift_nav_norsk_inntekt")
    @Enumerated(EnumType.STRING)
    private Vurderingsutfall_trygdeavgift_norsk_inntekt vurderingTrygdeavgiftNorskInntekt;

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Column(name = "trygdeavgift_nav_utenlandsk_inntekt")
    @Enumerated(EnumType.STRING)
    private Vurderingsutfall_trygdeavgift_utenlandsk_inntekt vurderingTrygdeavgiftUtenlandskInntekt;

    @Column(name = "bestemmelse", nullable = false)
    @Enumerated(EnumType.STRING)
    private Folketrygdloven_kap2_bestemmelser bestemmelse;

    @OneToMany(mappedBy = "medlemAvFolketrygden", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Collection<Medlemskapsperiode> medlemskapsperioder = new HashSet<>(1);

    @OneToOne(mappedBy = "medlemAvFolketrygden", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private FastsattTrygdeavgift fastsattTrygdeavgift;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public Vurderingsutfall_trygdeavgift_norsk_inntekt getVurderingTrygdeavgiftNorskInntekt() {
        return vurderingTrygdeavgiftNorskInntekt;
    }

    public void setVurderingTrygdeavgiftNorskInntekt(Vurderingsutfall_trygdeavgift_norsk_inntekt vurderingTrygdeavgiftNorskInntekt) {
        this.vurderingTrygdeavgiftNorskInntekt = vurderingTrygdeavgiftNorskInntekt;
    }

    public Vurderingsutfall_trygdeavgift_utenlandsk_inntekt getVurderingTrygdeavgiftUtenlandskInntekt() {
        return vurderingTrygdeavgiftUtenlandskInntekt;
    }

    public void setVurderingTrygdeavgiftUtenlandskInntekt(Vurderingsutfall_trygdeavgift_utenlandsk_inntekt vurderingTrygdeavgiftUtenlandskInntekt) {
        this.vurderingTrygdeavgiftUtenlandskInntekt = vurderingTrygdeavgiftUtenlandskInntekt;
    }

    public Collection<Medlemskapsperiode> getMedlemskapsperioder() {
        return medlemskapsperioder;
    }

    public void setMedlemskapsperioder(Collection<Medlemskapsperiode> medlemskapsperioder) {
        this.medlemskapsperioder = medlemskapsperioder;
    }

    public FastsattTrygdeavgift getFastsattTrygdeavgift() {
        return fastsattTrygdeavgift;
    }

    public void setFastsattTrygdeavgift(FastsattTrygdeavgift fastsattTrygdeavgift) {
        fastsattTrygdeavgift.setMedlemAvFolketrygden(this);
        this.fastsattTrygdeavgift = fastsattTrygdeavgift;
    }
}
