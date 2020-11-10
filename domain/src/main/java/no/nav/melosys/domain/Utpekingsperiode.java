package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

@Entity
@Table(name = "utpekingsperiode")
public class Utpekingsperiode implements PeriodeMedLovvalgsbestemmelse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fom;

    @Column(name = "tom_dato", nullable = true, updatable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalgsland", updatable = false)
    private Landkoder lovvalgsland;

    @Column(name = "lovvalgsbestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse bestemmelse;

    @Column(name = "tilleggsbestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse tilleggsbestemmelse;

    @Column(name = "medlperiode_id")
    private Long medlPeriodeID;

    @Column(name = "sendt_utland")
    private LocalDate sendtUtland;

    @SuppressWarnings("unused") // Trengs av Hibernate
    public Utpekingsperiode() {
    }

    public Utpekingsperiode(LocalDate fom, LocalDate tom, Landkoder lovvalgsland,
                            LovvalgBestemmelse bestemmelse, LovvalgBestemmelse tilleggsbestemmelse) {
        this.fom = fom;
        this.tom = tom;
        this.lovvalgsland = lovvalgsland;
        this.bestemmelse = bestemmelse;
        this.tilleggsbestemmelse = tilleggsbestemmelse;
    }

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

    @Override
    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    @Override
    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public Landkoder getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(Landkoder lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public LovvalgBestemmelse getBestemmelse() {
        return bestemmelse;
    }

    public void setBestemmelse(LovvalgBestemmelse bestemmelse) {
        this.bestemmelse = bestemmelse;
    }

    public LovvalgBestemmelse getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }

    public void setTilleggsbestemmelse(LovvalgBestemmelse tilleggsbestemmelse) {
        this.tilleggsbestemmelse = tilleggsbestemmelse;
    }

    public Long getMedlPeriodeID() {
        return medlPeriodeID;
    }

    public void setMedlPeriodeID(Long medlPeriodeID) {
        this.medlPeriodeID = medlPeriodeID;
    }

    public LocalDate getSendtUtland() {
        return sendtUtland;
    }

    public void setSendtUtland(LocalDate sendtUtland) {
        this.sendtUtland = sendtUtland;
    }

    @Override
    @Transient
    public Trygdedekninger getDekning() {
        return Trygdedekninger.UTEN_DEKNING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utpekingsperiode that = (Utpekingsperiode) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(behandlingsresultat, that.behandlingsresultat) &&
            Objects.equals(fom, that.fom) &&
            Objects.equals(tom, that.tom) &&
            lovvalgsland == that.lovvalgsland &&
            Objects.equals(bestemmelse, that.bestemmelse) &&
            Objects.equals(tilleggsbestemmelse, that.tilleggsbestemmelse) &&
            Objects.equals(medlPeriodeID, that.medlPeriodeID) &&
            Objects.equals(sendtUtland, that.sendtUtland);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingsresultat, fom, tom, lovvalgsland, bestemmelse, tilleggsbestemmelse, medlPeriodeID, sendtUtland);
    }
}
