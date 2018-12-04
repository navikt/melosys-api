package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_987_2009;
import no.nav.melosys.domain.bestemmelse.TilleggBestemmelse_883_2004;

@Entity
@Table(name = "lovvalg_periode")
public class Lovvalgsperiode implements ErPeriode {
    
    public static final class LovvalgBestemmelsekonverterer implements AttributeConverter<LovvalgBestemmelse, String> {

        @Override
        public String convertToDatabaseColumn(LovvalgBestemmelse attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public LovvalgBestemmelse convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            try {
                return LovvalgBestemmelse_883_2004.valueOf(dbData);
            } catch (IllegalArgumentException e) {
                // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
            }
            try {
                return LovvalgBestemmelse_987_2009.valueOf(dbData);
            } catch (IllegalArgumentException e) {
                // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
            }
            return TilleggBestemmelse_883_2004.valueOf(dbData);
        }

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;
    
    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fom;

    @Column(name = "tom_dato", nullable = false, updatable = false)
    private LocalDate tom;

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalgsland", nullable = false, updatable = false)
    private Landkoder lovvalgsland;

    @Column(name = "lovvalg_bestemmelse", nullable = false, updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse bestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "unntak_fra_lovvalgsland", updatable = false)
    private Landkoder unntakFraLovvalgsland;

    @Column(name = "unntak_fra_bestemmelse", updatable = false)
    @Convert(converter = LovvalgBestemmelsekonverterer.class)
    private LovvalgBestemmelse unntakFraBestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false, updatable = false)
    private InnvilgelsesResultat innvilgelsesresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype", nullable = false, updatable = false)
    private Medlemskapstype medlemskapstype;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning")
    private TrygdeDekning dekning;

    @Column(name = "medlperiode_id")
    private long medlPeriodeID;

    public long getId() {
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

    public InnvilgelsesResultat getInnvilgelsesresultat() {
        return innvilgelsesresultat;
    }

    public void setInnvilgelsesresultat(InnvilgelsesResultat innvilgelsesresultat) {
        this.innvilgelsesresultat = innvilgelsesresultat;
    }

    public Medlemskapstype getMedlemskapstype() {
        return medlemskapstype;
    }

    public void setMedlemskapstype(Medlemskapstype medlemskapstype) {
        this.medlemskapstype = medlemskapstype;
    }

    public TrygdeDekning getDekning() {
        return dekning;
    }

    public void setDekning(TrygdeDekning dekning) {
        this.dekning = dekning;
    }

    public void setMedlPeriodeID(long medlPeriodeID) {
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

}
