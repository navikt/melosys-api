package no.nav.melosys.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "BEHANDLING")
public class Behandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "behandling_id")
    private Long behandlingsId;

    @Column(name = "fagsak_id")
    private Long fagsakId;

    @ManyToOne
    @JoinColumn(name = "status", nullable = false)
    private BehandlingStatus status;

    @OneToOne
    @JoinColumn(name = "behandling_resultat_id")
    private Behandlingsresultat resultat;

    @ManyToOne
    @JoinColumn(name = "type", nullable = false)
    private BehandlingType type;

    public Long getBehandlingsId() {
        return behandlingsId;
    }

    public void setBehandlingsId(Long behandlingsId) {
        this.behandlingsId = behandlingsId;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public BehandlingType getType() {
        return type;
    }

    public void setType(BehandlingType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Behandling beh = (Behandling) o;
        return Objects.equals(behandlingsId, beh.getBehandlingsId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsId);
    }

}
