package no.nav.melosys.domain.datavarehus;

import javax.persistence.*;

@Entity
@Table(name = "behandling_dvh")
public class BehandlingDvh {

    @Id
    @SequenceGenerator(name = "behandling_dvh_sequence", sequenceName = "BEHANDLING_DVH_SEQ")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_dvh_sequence")
    @Column(name = "trans_id")
    private Long id;

    @Column(name = "id")
    private Long behandlingId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public static FagsakDvh.Builder builder() {
        return new FagsakDvh.Builder();
    }

    public static class Builder {

        public BehandlingDvh build() {
            return null;
        }
    }
}