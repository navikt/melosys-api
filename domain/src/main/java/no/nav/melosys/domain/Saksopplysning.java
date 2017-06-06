package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "SAKSOPPLYSNING")
public class Saksopplysning {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idGen")
    @SequenceGenerator(name = "idGen", sequenceName = "SEQ_SAKSOPPLYSNING")
    private Long id;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    @Column(name = "behandling_id", nullable = false)
    private Long behandlingId;

    @ManyToOne
    @JoinColumn(name = "kilde", nullable = false)
    private SaksopplysningKilde kilde;

    public Long getId() {
        return id;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public SaksopplysningKilde getKilde() {
        return kilde;
    }

    public void setKilde(SaksopplysningKilde kilde) {
        this.kilde = kilde;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Saksopplysning saksopplysning = (Saksopplysning) o;
        return Objects.equals(fagsakId, saksopplysning.getFagsakId())
                && Objects.equals(behandlingId, saksopplysning.getBehandlingId())
                && Objects.equals(kilde, saksopplysning.getKilde());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, behandlingId, kilde);
    }

}
