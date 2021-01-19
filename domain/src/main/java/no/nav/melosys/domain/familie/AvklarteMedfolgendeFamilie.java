package no.nav.melosys.domain.familie;

import java.util.Set;

public class AvklarteMedfolgendeFamilie {
    public final Set<OmfattetFamilie> familieOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetFamilie> familieIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeFamilie(Set<OmfattetFamilie> familieOmfattetAvNorskTrygd, Set<IkkeOmfattetFamilie> familieIkkeOmfattetAvNorskTrygd) {
        this.familieOmfattetAvNorskTrygd = familieOmfattetAvNorskTrygd;
        this.familieIkkeOmfattetAvNorskTrygd = familieIkkeOmfattetAvNorskTrygd;
    }
}
