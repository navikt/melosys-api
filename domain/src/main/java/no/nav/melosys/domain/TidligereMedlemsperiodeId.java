package no.nav.melosys.domain;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class TidligereMedlemsperiodeId implements Serializable {

    @Column(name = "behandling_id")
    private Long behandlingId;

    @Column(name = "periode_id")
    private Long periodeId;

    public TidligereMedlemsperiodeId() {}

    public TidligereMedlemsperiodeId(Long behandlingId, Long periodeId) {
        this.behandlingId = behandlingId;
        this.periodeId = periodeId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getPeriodeId() {
        return periodeId;
    }

    public void setPeriodeId(Long periodeId) {
        this.periodeId = periodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TidligereMedlemsperiodeId)) {
            return false;
        }
        TidligereMedlemsperiodeId that = (TidligereMedlemsperiodeId) o;
        return Objects.equals(this.behandlingId, that.behandlingId)
            && Objects.equals(this.periodeId, that.periodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, periodeId);
    }

}
