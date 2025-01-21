package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
import no.nav.melosys.domain.jpa.MedlemskapBestemmelsekonverter;
import no.nav.melosys.domain.kodeverk.Bestemmelse;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

@Entity
@Table(name = "medlemskapsperiode")
public class Medlemskapsperiode implements ErPeriode, HarBestemmelse<Bestemmelse> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandlingsresultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

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
    @Convert(converter = MedlemskapBestemmelsekonverter.class)
    private Bestemmelse bestemmelse;

    @OneToMany(mappedBy = "grunnlagMedlemskapsperiode", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Trygdeavgiftsperiode> trygdeavgiftsperioder = new HashSet<>(1);

    @Column(name = "medlperiode_id")
    private Long medlPeriodeID;

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

    public Bestemmelse getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(Bestemmelse bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public Long getMedlPeriodeID() {
        return medlPeriodeID;
    }

    public void setMedlPeriodeID(Long medlPeriodeID) {
        this.medlPeriodeID = medlPeriodeID;
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

    public boolean erFrivillig() {
        return medlemskapstype == Medlemskapstyper.FRIVILLIG;
    }

    public boolean erPliktig() {
        return medlemskapstype == Medlemskapstyper.PLIKTIG;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medlemskapsperiode that = (Medlemskapsperiode) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(behandlingsresultat, that.behandlingsresultat) &&
            Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom) &&
            innvilgelsesresultat == that.innvilgelsesresultat &&
            medlemskapstype == that.medlemskapstype &&
            trygdedekning == that.trygdedekning &&
            Objects.equals(medlPeriodeID, that.medlPeriodeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingsresultat, fom, tom, innvilgelsesresultat, medlemskapstype, trygdedekning, medlPeriodeID);
    }

    @Override
    public String toString() {
        return "Medlemskapsperiode{" +
            "id=" + id +
            ", behandlingsresultat=" + behandlingsresultat +
            ", fom=" + fom +
            ", tom=" + tom +
            ", innvilgelsesresultat=" + innvilgelsesresultat +
            ", medlemskapstype=" + medlemskapstype +
            ", trygdedekning=" + trygdedekning +
            ", medlPeriodeID=" + medlPeriodeID +
            '}';
    }

    public Set<Trygdeavgiftsperiode> getTrygdeavgiftsperioder() {
        return trygdeavgiftsperioder;
    }

    public void setTrygdeavgiftsperioder(Set<Trygdeavgiftsperiode> trygdeavgiftsperioder) {
        this.trygdeavgiftsperioder = trygdeavgiftsperioder;
    }

    public void addTrygdeavgiftsperiode(Trygdeavgiftsperiode trygdeavgiftsperiode) {
        trygdeavgiftsperiode.setGrunnlagMedlemskapsperiode(this);
        trygdeavgiftsperioder.add(trygdeavgiftsperiode);
    }

    public void avkortTomDato(int gjelderÅr) {
        if (this.overlapperMedÅr(gjelderÅr) && this.tom.getYear() > gjelderÅr) {
            this.tom = LocalDate.of(gjelderÅr, 12, 31);
        }
    }

    public void avkortFomDato(int gjelderÅr) {
        if (this.overlapperMedÅr(gjelderÅr) && this.fom.getYear() < gjelderÅr) {
            this.fom = LocalDate.of(gjelderÅr, 1, 1);
        }
    }

    public void clearTrygdeavgiftsperioder() {
        trygdeavgiftsperioder.stream().forEach(t -> t.setGrunnlagMedlemskapsperiode(null));
        trygdeavgiftsperioder.clear();
    }
}
