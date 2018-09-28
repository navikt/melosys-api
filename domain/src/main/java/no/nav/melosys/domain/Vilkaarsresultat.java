package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "vilkaarsresultat")
@EntityListeners(AuditingEntityListener.class)
public class Vilkaarsresultat extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkaar")
    private VilkaarType vilkaar;

    @Column(name = "oppfylt")
    private boolean oppfylt;

    @Column(name = "begrunnelse")
    private String begrunnelseKode;

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

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public void setBegrunnelseKode(String begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
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
        return Objects.equals(this.behandlingsresultat, that.behandlingsresultat)
            && Objects.equals(this.vilkaar, that.vilkaar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat, vilkaar);
    }

}
