package no.nav.melosys.domain.folketrygden;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

@Entity
@Table(name = "medlem_av_folketrygden")
public class MedlemAvFolketrygden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

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

    public Collection<Medlemskapsperiode> getMedlemskapsperioder() {
        return medlemskapsperioder;
    }

    public void setMedlemskapsperioder(Collection<Medlemskapsperiode> medlemskapsperioder) {
        this.medlemskapsperioder = medlemskapsperioder;
    }

    public void addMedlemskapsperiode(Medlemskapsperiode medlemskapsperiode) {
        this.medlemskapsperioder.add(medlemskapsperiode);
        medlemskapsperiode.setMedlemAvFolketrygden(this);
    }

    public void removeMedlemskapsperioder(Medlemskapsperiode medlemskapsperiode) {
        this.medlemskapsperioder.remove(medlemskapsperiode);
        medlemskapsperiode.setMedlemAvFolketrygden(null);
    }

    public FastsattTrygdeavgift getFastsattTrygdeavgift() {
        return fastsattTrygdeavgift;
    }

    public void setFastsattTrygdeavgift(FastsattTrygdeavgift fastsattTrygdeavgift) {
        this.fastsattTrygdeavgift = fastsattTrygdeavgift;
    }

    public Skatteplikttype utledSkatteplikttype() {
        return fastsattTrygdeavgift.getTrygdeavgiftsgrunnlag().getSkatteforholdTilNorge().stream().findFirst()
            .map(SkatteforholdTilNorge::getSkatteplikttype)
            .orElseThrow(() -> new RuntimeException("SkattepliktType ikke funnet, skal ikke skje for medlemAvFolketrygden :" + id));
    }


    public LocalDate utledMedlemskapsperiodeFom() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erInnvilget)
            .min(Comparator.comparing(Medlemskapsperiode::getFom))
            .map(Medlemskapsperiode::getFom).orElse(null);
    }

    public LocalDate utledMedlemskapsperiodeTom() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erInnvilget)
            .max(Comparator.comparing(Medlemskapsperiode::getTom))
            .map(Medlemskapsperiode::getTom).orElse(null);
    }


    public Folketrygdloven_kap2_bestemmelser utledMedlemskapsperiodeBestemmelse() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erInnvilget)
            .max(Comparator.comparing(Medlemskapsperiode::getBestemmelse))
            .map(Medlemskapsperiode::getBestemmelse).orElse(null);
    }

    public LocalDate utledOpphørtDato() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erOpphørt)
            .min(Comparator.comparing(Medlemskapsperiode::getFom))
            .map(Medlemskapsperiode::getFom).orElse(null);
    }
}
