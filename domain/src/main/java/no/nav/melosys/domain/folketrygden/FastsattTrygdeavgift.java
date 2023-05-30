package no.nav.melosys.domain.folketrygden;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer;
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker;

@Entity
@Table(name = "fastsatt_trygdeavgift")
public class FastsattTrygdeavgift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "medlem_av_folketrygden_id", nullable = false, updatable = false)
    private MedlemAvFolketrygden medlemAvFolketrygden;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private Trygdeavgift_typer trygdeavgiftstype;

    @OneToOne(mappedBy = "fastsattTrygdeavgift", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag;

    @OneToMany(mappedBy = "fastsattTrygdeavgift", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Trygdeavgiftsperiode> trygdeavgiftsperioder = new HashSet<>(1);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MedlemAvFolketrygden getMedlemAvFolketrygden() {
        return medlemAvFolketrygden;
    }

    public void setMedlemAvFolketrygden(MedlemAvFolketrygden medlemAvFolketrygden) {
        this.medlemAvFolketrygden = medlemAvFolketrygden;
    }

    public Trygdeavgift_typer getTrygdeavgiftstype() {
        return trygdeavgiftstype;
    }

    public void setTrygdeavgiftstype(Trygdeavgift_typer trygdeavgiftstype) {
        this.trygdeavgiftstype = trygdeavgiftstype;
    }

    public Trygdeavgiftsgrunnlag getTrygdeavgiftsgrunnlag() {
        return trygdeavgiftsgrunnlag;
    }

    public void setTrygdeavgiftsgrunnlag(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag) {
        trygdeavgiftsgrunnlag.setFastsattTrygdeavgift(this);
        this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag;
    }

    public Set<Trygdeavgiftsperiode> getTrygdeavgiftsperioder() {
        return trygdeavgiftsperioder;
    }

    public void setTrygdeavgiftsperioder(Set<Trygdeavgiftsperiode> trygdeavgiftsperioder) {
        this.trygdeavgiftsperioder = trygdeavgiftsperioder;
    }

    public boolean skalBetalesTilNav() {
        var trygdeavgiftMottaker = trygdeavgiftsgrunnlag.getTrygdeavgiftMottaker();
        return trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT;
    }
}
