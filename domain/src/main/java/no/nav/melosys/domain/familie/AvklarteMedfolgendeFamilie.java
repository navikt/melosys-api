package no.nav.melosys.domain.familie;

import java.util.Set;

public class AvklarteMedfolgendeFamilie {
    private final Set<OmfattetFamilie> familieOmfattetAvNorskTrygd;
    private final Set<IkkeOmfattetFamilie> familieIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeFamilie(Set<OmfattetFamilie> familieOmfattetAvNorskTrygd, Set<IkkeOmfattetFamilie> familieIkkeOmfattetAvNorskTrygd) {
        this.familieOmfattetAvNorskTrygd = familieOmfattetAvNorskTrygd;
        this.familieIkkeOmfattetAvNorskTrygd = familieIkkeOmfattetAvNorskTrygd;
    }

    public Set<OmfattetFamilie> getFamilieOmfattetAvNorskTrygd() {
        return familieOmfattetAvNorskTrygd;
    }

    public Set<IkkeOmfattetFamilie> getFamilieIkkeOmfattetAvNorskTrygd() {
        return familieIkkeOmfattetAvNorskTrygd;
    }
}
