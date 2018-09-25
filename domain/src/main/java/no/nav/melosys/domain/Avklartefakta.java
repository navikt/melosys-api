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

    @Column(name = "kontantytelse_type")
    private String kontantytelsestype;

    @Column(name = "offentlig_tjenestemann")
    private Boolean erOffentligTjenestemann;

    @Enumerated(EnumType.STRING)
    @Column(name = "bostedsland")
    private Landkoder bostedsland;

    @Column(name = "sokkel_skip")
    private String sokkelEllerSkip;

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

    public String getKontantytelsestype() {
        return kontantytelsestype;
    }

    public void setKontantytelsestype(String kontantytelsestype) {
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

    public String getSokkelEllerSkip() {
        return sokkelEllerSkip;
    }

    public void setSokkelEllerSkip(String sokkelEllerSkip) {
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
        if (this.id != 0 && that.id != 0) {
            return this.id == that.id;
        }
        return Objects.equals(this.arbeidsgiversForretningsland, that.arbeidsgiversForretningsland)
            && Objects.equals(this.mottarKontantytelse, that.mottarKontantytelse)
            && Objects.equals(this.kontantytelsestype, that.kontantytelsestype)
            && Objects.equals(this.erOffentligTjenestemann, that.erOffentligTjenestemann)
            && Objects.equals(this.bostedsland, that.bostedsland)
            && Objects.equals(this.sokkelEllerSkip, that.sokkelEllerSkip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiversForretningsland, mottarKontantytelse, kontantytelsestype,
            erOffentligTjenestemann, bostedsland, sokkelEllerSkip);
    }
}
