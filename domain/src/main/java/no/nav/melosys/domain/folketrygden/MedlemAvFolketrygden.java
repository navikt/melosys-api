package no.nav.melosys.domain.folketrygden;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

import jakarta.persistence.*;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;

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
        var trygdeavgiftsperiode = fastsattTrygdeavgift.getTrygdeavgiftsperioder().stream().findFirst();
        if (trygdeavgiftsperiode.isEmpty() && erÅpenSluttdato()) {
            return Skatteplikttype.SKATTEPLIKTIG;
        } else if (trygdeavgiftsperiode.isEmpty()) {
            throw new RuntimeException("Trygdeavgiftsperiode ikke funnet, og det er ikke åpen sluttdato, id = " + id);
        }

        return trygdeavgiftsperiode.get().getGrunnlagSkatteforholdTilNorge().getSkatteplikttype();
    }


    public LocalDate utledMedlemskapsperiodeFom() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erInnvilget)
            .min(Comparator.comparing(Medlemskapsperiode::getFom))
            .map(Medlemskapsperiode::getFom)
            .orElse(null);
    }

    public LocalDate utledMedlemskapsperiodeTom() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erInnvilget)
            .filter(medlemskapsperiode -> medlemskapsperiode.getTom() != null)
            .max(Comparator.comparing(Medlemskapsperiode::getTom))
            .map(Medlemskapsperiode::getTom)
            .orElse(null);
    }

    public LocalDate utledOpphørtDato() {
        return medlemskapsperioder.stream()
            .filter(Medlemskapsperiode::erOpphørt)
            .min(Comparator.comparing(Medlemskapsperiode::getFom))
            .map(Medlemskapsperiode::getFom)
            .orElse(null);
    }

    /*
    Åpen sluttdato på medlemskapsperiode er tillatt for arbeidsland Norge og bestemmelse 2.1. Skal ikke ha trygdeavgiftsperioder.
     */
    private boolean erÅpenSluttdato() {
        return fastsattTrygdeavgift.getMedlemAvFolketrygden().utledMedlemskapsperiodeTom() == null;
    }
}
