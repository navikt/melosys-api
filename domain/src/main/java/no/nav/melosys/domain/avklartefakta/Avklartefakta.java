package no.nav.melosys.domain.avklartefakta;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;

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
    private Avklartefaktatype type;

    @Column(name = "referanse")
    private String referanse;

    @Column(name = "subjekt")
    private String subjekt;

    @Column(name = "fakta")
    private String fakta;

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    @OneToMany(mappedBy = "avklartefakta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AvklartefaktaRegistrering> registreringer = new HashSet<>();

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public Avklartefaktatype getType() {
        return type;
    }

    public void setType(Avklartefaktatype type) {
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

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public void setRegistreringer(Set<AvklartefaktaRegistrering> registreringer) {
        this.registreringer = registreringer;
    }

    public void oppdaterRegistreringer(Set<AvklartefaktaRegistrering> nyeRegistreringer) {
        this.registreringer.addAll(nyeRegistreringer);
        this.registreringer.retainAll(nyeRegistreringer);
    }

    public Set<AvklartefaktaRegistrering> getRegistreringer() {
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
        return Objects.equals(this.behandlingsresultat, that.getBehandlingsresultat()) &&
               Objects.equals(this.type, that.getType()) &&
               Objects.equals(this.subjekt, that.getSubjekt()) &&
               Objects.equals(this.referanse, that.getReferanse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat, type, subjekt, referanse);
    }

}
