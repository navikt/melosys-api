package no.nav.melosys.domain;

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "medlemskapsperiode")
public class Medlemskapsperiode implements ErPeriode, HarBestemmelse<Folketrygdloven_kap2_bestemmelser> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medlem_av_folketrygden_id", nullable = false, updatable = false)
    private MedlemAvFolketrygden medlemAvFolketrygden;

    @Column(name = "fom_dato", nullable = false)
    private LocalDate fom;

    @Column(name = "tom_dato")
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false)
    private InnvilgelsesResultat innvilgelsesresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype", nullable = false)
    private Medlemskapstyper medlemskapstype;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning", nullable = false)
    private Trygdedekninger trygdedekning;

    @Column(name = "bestemmelse", nullable = false)
    @Enumerated(EnumType.STRING)
    private Folketrygdloven_kap2_bestemmelser bestemmelse;

    @Column(name = "medlperiode_id")
    private Long medlPeriodeID;

    @OneToMany(mappedBy = "grunnlagMedlemskapsperiode", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Collection<Trygdeavgiftsperiode> trygdeavgiftsperioder = new HashSet<>(1);

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

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public InnvilgelsesResultat getInnvilgelsesresultat() {
        return innvilgelsesresultat;
    }

    public void setInnvilgelsesresultat(InnvilgelsesResultat innvilgelsesresultat) {
        this.innvilgelsesresultat = innvilgelsesresultat;
    }

    public Medlemskapstyper getMedlemskapstype() {
        return medlemskapstype;
    }

    public void setMedlemskapstype(Medlemskapstyper medlemskapstype) {
        this.medlemskapstype = medlemskapstype;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(Trygdedekninger trygdedekning) {
        this.trygdedekning = trygdedekning;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public Long getMedlPeriodeID() {
        return medlPeriodeID;
    }

    public void setMedlPeriodeID(Long medlPeriodeID) {
        this.medlPeriodeID = medlPeriodeID;
    }

    public Collection<Trygdeavgiftsperiode> getTrygdeavgiftsperioder() {
        return trygdeavgiftsperioder;
    }

    @Deprecated(since = "Bare for test...", forRemoval = false)
    public void setTrygdeavgiftsperioder(Collection<Trygdeavgiftsperiode> trygdeavgiftsperioder) {
        this.trygdeavgiftsperioder = trygdeavgiftsperioder;
    }

    public boolean erInnvilget() {
        return innvilgelsesresultat == InnvilgelsesResultat.INNVILGET;
    }

    public boolean erOpphørt() {
        return innvilgelsesresultat == InnvilgelsesResultat.OPPHØRT;
    }

    public boolean erAvslaatt() {
        return innvilgelsesresultat == InnvilgelsesResultat.AVSLAATT;
    }

    public boolean erHelsedel() {
        return List.of(
            Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
        ).contains(trygdedekning);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medlemskapsperiode that = (Medlemskapsperiode) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(medlemAvFolketrygden, that.medlemAvFolketrygden) &&
            Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom) &&
            innvilgelsesresultat == that.innvilgelsesresultat &&
            medlemskapstype == that.medlemskapstype &&
            trygdedekning == that.trygdedekning &&
            Objects.equals(medlPeriodeID, that.medlPeriodeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, medlemAvFolketrygden, fom, tom, innvilgelsesresultat, medlemskapstype, trygdedekning, medlPeriodeID);
    }

    @Override
    public String toString() {
        return "Medlemskapsperiode{" +
            "id=" + id +
            ", medlemAvFolketrygden=" + medlemAvFolketrygden +
            ", fom=" + fom +
            ", tom=" + tom +
            ", innvilgelsesresultat=" + innvilgelsesresultat +
            ", medlemskapstype=" + medlemskapstype +
            ", trygdedekning=" + trygdedekning +
            ", medlPeriodeID=" + medlPeriodeID +
            '}';
    }
}
