package no.nav.melosys.domain;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TidligereMedlemsperiode)) return false;
        TidligereMedlemsperiode that = (TidligereMedlemsperiode) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
