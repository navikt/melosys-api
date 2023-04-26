package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.avgift.TrygdeavgiftDeprecated;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.DELVIS_INNVILGET;
import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET;

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

    @Deprecated(since = "Dobbeltsjekk om denne kan fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Column(name = "arbeidsland", nullable = false)
    private String arbeidsland;

    @Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
    @Enumerated(EnumType.STRING)
    @Column(name = "bestemmelse", nullable = false)
    private Folketrygdloven_kap2_bestemmelser bestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false)
    private InnvilgelsesResultat innvilgelsesresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype", nullable = false)
    private Medlemskapstyper medlemskapstype;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning", nullable = false)
    private Trygdedekninger trygdedekning;

    @Column(name = "medlperiode_id")
    private Long medlPeriodeID;

    @OneToMany(mappedBy = "medlemskapsperiode", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Collection<TrygdeavgiftDeprecated> trygdeavgiftDeprecated = new ArrayList<>(1);

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

    public String getArbeidsland() {
        return arbeidsland;
    }

    public void setArbeidsland(String arbeidsland) {
        this.arbeidsland = arbeidsland;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return medlemAvFolketrygden.getBestemmelse();
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

    public Long getMedlPeriodeID() {
        return medlPeriodeID;
    }

    public void setMedlPeriodeID(Long medlPeriodeID) {
        this.medlPeriodeID = medlPeriodeID;
    }

    public Collection<TrygdeavgiftDeprecated> getTrygdeavgift() {
        return trygdeavgiftDeprecated;
    }

    public void setTrygdeavgift(Collection<TrygdeavgiftDeprecated> trygdeavgiftDeprecated) {
        this.trygdeavgiftDeprecated = trygdeavgiftDeprecated;
    }

    public boolean erInnvilget() {
        return innvilgelsesresultat == INNVILGET || innvilgelsesresultat == DELVIS_INNVILGET;
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
            Objects.equals(arbeidsland, that.arbeidsland) &&
            bestemmelse == that.bestemmelse &&
            innvilgelsesresultat == that.innvilgelsesresultat &&
            medlemskapstype == that.medlemskapstype &&
            trygdedekning == that.trygdedekning &&
            Objects.equals(medlPeriodeID, that.medlPeriodeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, medlemAvFolketrygden, fom, tom, arbeidsland, bestemmelse, innvilgelsesresultat, medlemskapstype, trygdedekning, medlPeriodeID);
    }

    @Override
    public String toString() {
        return "Medlemskapsperiode{" +
            "id=" + id +
            ", medlemAvFolketrygden=" + medlemAvFolketrygden +
            ", fom=" + fom +
            ", tom=" + tom +
            ", arbeidsland='" + arbeidsland + '\'' +
            ", bestemmelse=" + bestemmelse +
            ", innvilgelsesresultat=" + innvilgelsesresultat +
            ", medlemskapstype=" + medlemskapstype +
            ", trygdedekning=" + trygdedekning +
            ", medlPeriodeID=" + medlPeriodeID +
            '}';
    }
}
