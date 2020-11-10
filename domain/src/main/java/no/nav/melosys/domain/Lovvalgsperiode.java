package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;

@Entity
@Table(name = "lovvalg_periode")
public class Lovvalgsperiode implements PeriodeMedLovvalgsbestemmelse {
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

    @Column(name = "lovvalg_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse bestemmelse;

    @Column(name = "tillegg_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse tilleggsbestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false, updatable = false)
    private InnvilgelsesResultat innvilgelsesresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype", updatable = false)
    private Medlemskapstyper medlemskapstype;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning")
    private Trygdedekninger dekning;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Lovvalgsperiode)) {
            return false;
        }
        Lovvalgsperiode that = (Lovvalgsperiode) o;
        return Objects.equals(this.behandlingsresultat, that.behandlingsresultat)
            && Objects.equals(this.fom, that.fom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat, fom);
    }

    @Override
    public String toString() {
        return "Lovvalgsperiode{" +
            "id=" + id +
            ", behandlingsresultat=" + behandlingsresultat +
            ", fom=" + fom +
            ", tom=" + tom +
            ", lovvalgsland=" + lovvalgsland +
            ", bestemmelse=" + bestemmelse +
            ", tilleggsbestemmelse=" + tilleggsbestemmelse +
            ", innvilgelsesresultat=" + innvilgelsesresultat +
            ", medlemskapstype=" + medlemskapstype +
            ", dekning=" + dekning +
            ", medlPeriodeID=" + medlPeriodeID +
            '}';
    }

    public boolean erInnvilget() {
        return getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET;
    }

    public boolean erAvslått() {
        return getInnvilgelsesresultat() == InnvilgelsesResultat.AVSLAATT;
    }

    public boolean harUgyldigTilstand() {
        return !erInnvilget() && !erAvslått();
    }

    public static Lovvalgsperiode av(AnmodningsperiodeSvar anmodningsperiodeSvar,
                                     Medlemskapstyper medlemskapstype) throws FunksjonellException {
        if (anmodningsperiodeSvar == null) {
            throw new FunksjonellException("Kan ikke opprette lovvalgsperiode fra anmodningsperiode " +
                "uten at et svar er registrert!");
        }

        Anmodningsperiode anmodningsperiode = anmodningsperiodeSvar.getAnmodningsperiode();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(anmodningsperiode.getBestemmelse());

        if (anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == Anmodningsperiodesvartyper.DELVIS_INNVILGELSE) {
            lovvalgsperiode.setFom(anmodningsperiodeSvar.getInnvilgetFom());
            lovvalgsperiode.setTom(anmodningsperiodeSvar.getInnvilgetTom());
        } else {
            lovvalgsperiode.setFom(anmodningsperiode.getFom());
            lovvalgsperiode.setTom(anmodningsperiode.getTom());
        }

        InnvilgelsesResultat innvilgelsesResultat = anmodningsperiodeSvar.getAnmodningsperiodeSvarType() == Anmodningsperiodesvartyper.AVSLAG ?
            InnvilgelsesResultat.AVSLAATT : InnvilgelsesResultat.INNVILGET;
        lovvalgsperiode.setInnvilgelsesresultat(innvilgelsesResultat);

        lovvalgsperiode.setMedlemskapstype(medlemskapstype);
        lovvalgsperiode.setMedlPeriodeID(anmodningsperiode.getMedlPeriodeID());
        lovvalgsperiode.setTilleggsbestemmelse(anmodningsperiode.getTilleggsbestemmelse());
        if (innvilgelsesResultat != InnvilgelsesResultat.AVSLAATT) {
            lovvalgsperiode.setLovvalgsland(anmodningsperiode.getLovvalgsland());
        }
        lovvalgsperiode.setDekning(anmodningsperiode.getDekning());
        return lovvalgsperiode;
    }

    public static Lovvalgsperiode av(Utpekingsperiode utpekingsperiode) {

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(utpekingsperiode.getFom());
        lovvalgsperiode.setTom(utpekingsperiode.getTom());
        lovvalgsperiode.setBestemmelse(utpekingsperiode.getBestemmelse());
        lovvalgsperiode.setTilleggsbestemmelse(utpekingsperiode.getTilleggsbestemmelse());
        lovvalgsperiode.setLovvalgsland(utpekingsperiode.getLovvalgsland());
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.UNNTATT);
        lovvalgsperiode.setMedlPeriodeID(utpekingsperiode.getMedlPeriodeID());
        return lovvalgsperiode;
    }
}
