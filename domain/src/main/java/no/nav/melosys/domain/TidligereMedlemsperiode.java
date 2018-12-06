package no.nav.melosys.domain;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "tidligere_medlemsperiode")
public class TidligereMedlemsperiode {

    @EmbeddedId
    private TidligereMedlemsperiodeId id;

    public TidligereMedlemsperiode() {}

    public TidligereMedlemsperiode(long behandlingId, long periodeId) {
        id = new TidligereMedlemsperiodeId(behandlingId, periodeId);
    }

    public TidligereMedlemsperiodeId getId() {
        return id;
    }

    public void setId(TidligereMedlemsperiodeId id) {
        this.id = id;
    }
}
