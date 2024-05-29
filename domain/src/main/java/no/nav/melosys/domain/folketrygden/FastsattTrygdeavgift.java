package no.nav.melosys.domain.folketrygden;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import no.nav.melosys.domain.avgift.Inntektsperiode;
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge;
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer;

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

    @OneToMany(mappedBy = "fastsattTrygdeavgift", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
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

    public Set<Trygdeavgiftsperiode> getTrygdeavgiftsperioder() {
        return trygdeavgiftsperioder;
    }

    public void setTrygdeavgiftsperioder(Set<Trygdeavgiftsperiode> trygdeavgiftsperioder) {
        this.trygdeavgiftsperioder = trygdeavgiftsperioder;
    }

    public Set<SkatteforholdTilNorge> hentSkatteforholdTilNorge() {
        return getTrygdeavgiftsperioder().stream()
            .map(Trygdeavgiftsperiode::getGrunnlagSkatteforholdTilNorge)
            .collect(Collectors.toSet());
    }

    public Set<Inntektsperiode> hentInntektsperioder() {
        return getTrygdeavgiftsperioder().stream()
            .map(Trygdeavgiftsperiode::getGrunnlagInntekstperiode)
            .collect(Collectors.toSet());
    }

}
