package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.medlemskap.DekningMedl;
import no.nav.melosys.domain.dokument.medlemskap.GrunnlagMedl;
import no.nav.melosys.exception.TekniskException;

@Entity
@Table(name = "lovvalg_periode")
public class Lovvalgsperiode implements ErPeriode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "bruker_id", updatable = false)
    private String brukerID; //AktørID

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

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalg_bestemmelse", nullable = false, updatable = false)
    private LovvalgBestemmelse_883_2004 bestemmelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false, updatable = false)
    private InnvilgelsesResultat innvilgelsesresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype", nullable = false, updatable = false)
    private Medlemskapstype medlemskapstype;

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning")
    private TrygdeDekning dekning;

    public long getId() {
        return id;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public void setBrukerID(String brukerID) {
        this.brukerID = brukerID;
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

    public void setBestemmelse(LovvalgBestemmelse_883_2004 bestemmelse) {
        this.bestemmelse = bestemmelse;
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

    public DekningMedl hentFellesKodeForTrygdDekningtype() throws TekniskException {
        DekningMedl dekningMedltype;
        switch (dekning) {
            case FULL_DEKNING_EOSFO:
                dekningMedltype = DekningMedl.FULL;
                break;
            case UTEN_DEKNING:
                dekningMedltype = DekningMedl.UNNTATT;
            break;
            default:
                throw new TekniskException("Dekningstype støttes ikke:" + dekning.getKode());
        }
        return dekningMedltype;
    }

    public GrunnlagMedl hentFellesKodeForGrunnlagMedltype() throws TekniskException {
        GrunnlagMedl grunnlagMedltype;
        switch (bestemmelse) {
            //Article 11
            case ART11_3A:
                grunnlagMedltype = GrunnlagMedl.FO_11_3_A;
                break;
            case ART11_3B:
                grunnlagMedltype = GrunnlagMedl.FO_11_3_B;
                break;
            case ART11_3C:
                grunnlagMedltype = GrunnlagMedl.FO_11_3_C;
                break;
            case ART11_3E:
                grunnlagMedltype = GrunnlagMedl.FO_11_3_E;
                break;

            //Article 12
            case ART12_1:
                grunnlagMedltype = GrunnlagMedl.FO_12_1;
                break;
            case ART12_2:
                grunnlagMedltype = GrunnlagMedl.FO_12_2;
                break;

            //Article 13
            case ART13_1A:
                grunnlagMedltype = GrunnlagMedl.FO_13_1_A;
                break;

            case ART13_1B1:
                grunnlagMedltype = GrunnlagMedl.FO_13_1_B;
                break;

            case ART13_1B2:
                grunnlagMedltype = GrunnlagMedl.FO_13_B_II;
                break;

            case ART13_1B3:
                grunnlagMedltype = GrunnlagMedl.FO_13_B_III;
                break;

            case ART13_1B4:
                grunnlagMedltype = GrunnlagMedl.FO_13_B_IV;
                break;

            case ART13_2A:
                grunnlagMedltype = GrunnlagMedl.FO_13_2_A;
                break;

            case ART13_2B:
                grunnlagMedltype = GrunnlagMedl.FO_13_2_B;
                break;

            case ART13_3:
                grunnlagMedltype = GrunnlagMedl.FO_13_3;
                break;

            case ART13_4:
                grunnlagMedltype = GrunnlagMedl.FO_13_4;
                break;

            default:
                throw new TekniskException("Lovvalgsbestemmelse støttes ikke i MEDL. Kode: " + bestemmelse.getKode() + " Beskrivelse: " + bestemmelse.getBeskrivelse());
        }
    return grunnlagMedltype;
    }
}
