package no.nav.melosys.domain;

import java.util.Objects;
import javax.persistence.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "avklartefakta_registrering")
@EntityListeners(AuditingEntityListener.class)
public class AvklartefaktaRegistrering extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avklartefakta_id ", nullable = false, updatable = false)
    private Avklartefakta avklartefakta;

    @Column(name = "begrunnelse")
    private String begrunnelseKode;

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    public Avklartefakta getAvklartefakta() {
        return avklartefakta;
    }

    public void setAvklartefakta(Avklartefakta avklartefakta) {
        this.avklartefakta = avklartefakta;
    }

    public String getBegrunnelseKode() { return begrunnelseKode; }

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
        if (!(o instanceof AvklartefaktaRegistrering)) {
            return false;
        }
        AvklartefaktaRegistrering that = (AvklartefaktaRegistrering) o;
        return Objects.equals(this.avklartefakta, that.avklartefakta) &&
                (Objects.equals(this.begrunnelseKode, that.getBegrunnelseKode()) ||
                 Objects.equals(this.begrunnelseFritekst, that.getBegrunnelseFritekst()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(avklartefakta, begrunnelseKode, begrunnelseFritekst);
    }
}
