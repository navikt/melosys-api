package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "vilkaarsresultat")
@EntityListeners(AuditingEntityListener.class)
public class Vilkaarsresultat extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkaar")
    private VilkaarType vilkaar;

    @Column(name = "oppfylt")
    private boolean oppfylt;

    @OneToMany(mappedBy = "vilkaarsresultat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VilkaarBegrunnelse> begrunnelser = new HashSet<>();

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public void setBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public VilkaarType getVilkaar() {
        return vilkaar;
    }

    public void setVilkaar(VilkaarType vilkaar) {
        this.vilkaar = vilkaar;
    }

    public boolean isOppfylt() {
        return oppfylt;
    }

    public void setOppfylt(boolean oppfylt) {
        this.oppfylt = oppfylt;
    }

    public Set<VilkaarBegrunnelse> getBegrunnelser() {
        return begrunnelser;
    }

    public void setBegrunnelser(Set<VilkaarBegrunnelse> begrunnelser) {
        this.begrunnelser = begrunnelser;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vilkaarsresultat)) {
            return false;
        }
        Vilkaarsresultat that = (Vilkaarsresultat) o;
        return Objects.equals(getBehandlingsresultat(), that.getBehandlingsresultat())
            && Objects.equals(getVilkaar(), that.getVilkaar());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandlingsresultat(), getVilkaar());
    }

}
