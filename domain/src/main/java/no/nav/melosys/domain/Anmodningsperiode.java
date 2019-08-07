package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

@Entity
@Table(name = "anmodningsperiode")
public class Anmodningsperiode implements ErPeriodeMedBestemmelse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fom;

    @Column(name = "tom_dato", nullable = false, updatable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalgsland", updatable = false)
    private Landkoder lovvalgsland;

    @Column(name = "lovvalg_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse bestemmelse;

    @Column(name = "tillegg_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse tilleggsbestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "unntak_fra_lovvalgsland", updatable = false)
    private Landkoder unntakFraLovvalgsland;

    @Column(name = "unntak_fra_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse unntakFraBestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning")
    private Trygdedekninger dekning;

    @Column(name = "medlperiode_id")
    private Long medlPeriodeID;

    @Column(name = "sendt")
    private boolean sendt;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "anmodningsperiode")
    private AnmodningsperiodeSvar anmodningsperiodeSvar;

    @SuppressWarnings("unused") // Trengs av Hibernate
    public Anmodningsperiode() {
    }

    public Anmodningsperiode(LocalDate fom, LocalDate tom, Landkoder lovvalgsland, LovvalgBestemmelse bestemmelse, LovvalgBestemmelse tilleggsbestemmelse,
                             Landkoder unntakFraLovvalgsland, LovvalgBestemmelse unntakFraBestemmelse) {
        this.fom = fom;
        this.tom = tom;
        this.lovvalgsland = lovvalgsland;
        this.bestemmelse = bestemmelse;
        this.tilleggsbestemmelse = tilleggsbestemmelse;
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
        this.unntakFraBestemmelse = unntakFraBestemmelse;
    }

    public Long getId() {
        return id;
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

    @Override
    public LocalDate getTom() {
        return tom;
    }

    public Landkoder getLovvalgsland() {
        return lovvalgsland;
    }

    public LovvalgBestemmelse getBestemmelse() {
        return bestemmelse;
    }

    public LovvalgBestemmelse getTilleggsbestemmelse() {
        return tilleggsbestemmelse;
    }

    public Landkoder getUnntakFraLovvalgsland() {
        return unntakFraLovvalgsland;
    }

    public void setUnntakFraLovvalgsland(Landkoder unntakFraLovvalgsland) {
        this.unntakFraLovvalgsland = unntakFraLovvalgsland;
    }

    public LovvalgBestemmelse getUnntakFraBestemmelse() {
        return unntakFraBestemmelse;
    }

    public void setUnntakFraBestemmelse(LovvalgBestemmelse unntakFraBestemmelse) {
        this.unntakFraBestemmelse = unntakFraBestemmelse;
    }

    public Trygdedekninger getDekning() {
        return dekning;
    }

    public void setDekning(Trygdedekninger dekning) {
        this.dekning = dekning;
    }

    public Long getMedlPeriodeID() {
        return medlPeriodeID;
    }

    public void setMedlPeriodeID(Long medlPeriodeID) {
        this.medlPeriodeID = medlPeriodeID;
    }

    public boolean erSendt() {
        return sendt;
    }

    public void setSendt(boolean sendt) {
        this.sendt = sendt;
    }

    public AnmodningsperiodeSvar getAnmodningsperiodeSvar() {
        return anmodningsperiodeSvar;
    }

    public void setAnmodningsperiodeSvar(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        this.anmodningsperiodeSvar = anmodningsperiodeSvar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Anmodningsperiode)) {
            return false;
        }
        Anmodningsperiode that = (Anmodningsperiode) o;
        return Objects.equals(this.behandlingsresultat, that.behandlingsresultat)
            && Objects.equals(this.fom, that.fom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat, fom);
    }

    public boolean gjelderSammeLandOgUnntakSom(Anmodningsperiode periode2) {
        return lovvalgsland == periode2.getLovvalgsland() &&
            unntakFraBestemmelse == periode2.getUnntakFraBestemmelse() &&
            unntakFraLovvalgsland == periode2.getUnntakFraLovvalgsland();
    }
}
