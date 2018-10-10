package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "avklartefakta")
public class Avklartefakta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name="beh_resultat_id")
    private Behandlingsresultat behandlingsresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AvklartefaktaType type;

    @Column(name = "referanse")
    private String referanse;

    @Column(name = "subjekt")
    private String subjekt;

    @Column(name = "fakta")
    private String fakta;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "avklartefakta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AvklartefaktaRegistrering> registreringer;

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public AvklartefaktaType getAvklartefaktakode() {
        return type;
    }

    public void setAvklartefaktakode(AvklartefaktaType type) {
        this.type = type;
    }

    public String getReferanse() {
        return referanse;
    }

    public void setReferanse(String referanse) {
        this.referanse = referanse;
    }

    public String getSubjekt() {
        return subjekt;
    }

    public void setSubjekt(String subjekt) {
        this.subjekt = subjekt;
    }

    public void setFakta(String fakta) {
        this.fakta = fakta;
    }

    public String getFakta() {
        return fakta;
    }

    public void setRegistreringer(Set<AvklartefaktaRegistrering> registreringer) {
        this.registreringer = registreringer;
    }

    public void oppdaterRegistreringer(Set<AvklartefaktaRegistrering> nyeRegistreringer) {
        if (this.registreringer == null) {
            this.registreringer = nyeRegistreringer;
            return;
        }

        nyeRegistreringer.forEach(r -> this.registreringer.add(r));
        this.registreringer.retainAll(nyeRegistreringer);
    }

    public Set<AvklartefaktaRegistrering> getRegistreringer() {
        if (registreringer == null) {
            return new HashSet<>();
        }
        return registreringer;
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
        return Objects.equals(this.behandlingsresultat, that.behandlingsresultat) &&
                (Objects.equals(this.referanse, that.getReferanse()) ||
                Objects.equals(this.type, that.getAvklartefaktakode()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat);
    }
}
