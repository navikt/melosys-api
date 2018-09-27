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

    @Enumerated(EnumType.STRING)
    @Column(name = "fakta_type")
    private AvklartefaktaType type;

    @Column(name = "begrunnelse")
    private String begrunnelseKode;

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    public Avklartefakta getAvklartefakta() {
        return avklartefakta;
    }

    public AvklartefaktaType getType() {
        return type;
    }

    public void setType(AvklartefaktaType type) {
        this.type = type;
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
        if (!(o instanceof AvklartefaktaRegistrering)) {
            return false;
        }
        AvklartefaktaRegistrering that = (AvklartefaktaRegistrering) o;
        return Objects.equals(this.avklartefakta, that.avklartefakta)
            && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avklartefakta, type);
    }
}
