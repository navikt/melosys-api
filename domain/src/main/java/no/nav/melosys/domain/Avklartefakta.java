package no.nav.melosys.domain;

import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "avklartefakta")
public class Avklartefakta {

    // Populeres av Hibernate med behandlingsresultat.id
    @Id
    private long id;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name="beh_resultat_id")
    private Behandlingsresultat behandlingsresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "arbeidsgiver_forretningsland")
    private Landkoder arbeidsgiversForretningsland;

    @Column(name = "mottar_kontantytelse")
    private Boolean mottarKontantytelse;

    @Enumerated(EnumType.STRING)
    @Column(name = "kontantytelse_type")
    private KontantytelseType kontantytelsestype;

    @Column(name = "offentlig_tjenestemann")
    private Boolean erOffentligTjenestemann;

    @Enumerated(EnumType.STRING)
    @Column(name = "bostedsland")
    private Landkoder bostedsland;

    @Enumerated(EnumType.STRING)
    @Column(name = "sokkel_skip")
    private SokkelEllerSkip sokkelEllerSkip;

    @OneToMany(mappedBy = "avklartefakta")
    private Set<AvklartefaktaRegistrering> registrering;

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public Landkoder getArbeidsgiversForretningsland() {
        return arbeidsgiversForretningsland;
    }

    public void setArbeidsgiversForretningsland(Landkoder arbeidsgiversForretningsland) {
        this.arbeidsgiversForretningsland = arbeidsgiversForretningsland;
    }

    public Boolean getMottarKontantytelse() {
        return mottarKontantytelse;
    }

    public void setMottarKontantytelse(Boolean mottarKontantytelse) {
        this.mottarKontantytelse = mottarKontantytelse;
    }

    public KontantytelseType getKontantytelsestype() {
        return kontantytelsestype;
    }

    public void setKontantytelsestype(KontantytelseType kontantytelsestype) {
        this.kontantytelsestype = kontantytelsestype;
    }

    public Boolean getErOffentligTjenestemann() {
        return erOffentligTjenestemann;
    }

    public void setErOffentligTjenestemann(Boolean erOffentligTjenestemann) {
        this.erOffentligTjenestemann = erOffentligTjenestemann;
    }

    public Landkoder getBostedsland() {
        return bostedsland;
    }

    public void setBostedsland(Landkoder bostedsland) {
        this.bostedsland = bostedsland;
    }

    public SokkelEllerSkip getSokkelEllerSkip() {
        return sokkelEllerSkip;
    }

    public void setSokkelEllerSkip(SokkelEllerSkip sokkelEllerSkip) {
        this.sokkelEllerSkip = sokkelEllerSkip;
    }

    public Set<AvklartefaktaRegistrering> getRegistrering() {
        return registrering;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Avklartefakta)) {
            return false;
        }
        Avklartefakta that = (Avklartefakta) o;
        return Objects.equals(this.behandlingsresultat, that.behandlingsresultat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat);
    }
}
