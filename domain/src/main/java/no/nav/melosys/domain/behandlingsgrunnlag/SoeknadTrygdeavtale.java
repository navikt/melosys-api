package no.nav.melosys.domain.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.RepresentantIUtlandet;

public class SoeknadTrygdeavtale extends Soeknad {
    private RepresentantIUtlandet representantIUtlandet;

    public RepresentantIUtlandet getRepresentantIUtlandet() {
        return representantIUtlandet;
    }

    public void setRepresentantIUtlandet(RepresentantIUtlandet representantIUtlandet) {
        this.representantIUtlandet = representantIUtlandet;
    }
}
