package no.nav.melosys.domain.avklartefakta;

import java.util.Objects;
import jakarta.persistence.*;

import no.nav.melosys.domain.RegistreringsInfo;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "avklartefakta_registrering")
@EntityListeners(AuditingEntityListener.class)
public class AvklartefaktaRegistrering extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "avklartefakta_id ", nullable = false, updatable = false)
    private Avklartefakta avklartefakta;

    @Column(name = "begrunnelse")
    private String begrunnelseKode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Avklartefakta getAvklartefakta() {
        return avklartefakta;
    }

    public void setAvklartefakta(Avklartefakta avklartefakta) {
        this.avklartefakta = avklartefakta;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public void setBegrunnelseKode(String begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
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
        return Objects.equals(this.avklartefakta, that.getAvklartefakta()) &&
                Objects.equals(this.begrunnelseKode, that.getBegrunnelseKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(avklartefakta, begrunnelseKode);
    }
}
