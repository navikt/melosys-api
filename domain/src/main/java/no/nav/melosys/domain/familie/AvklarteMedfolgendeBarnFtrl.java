package no.nav.melosys.domain.familie;

import java.util.Set;

public class AvklarteMedfolgendeBarnFtrl {
    public final Set<OmfattetFamilie> barnOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetBarnFtrl> barnIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeBarnFtrl(Set<OmfattetFamilie> barnOmfattetAvNorskTrygd, Set<IkkeOmfattetBarnFtrl> barnIkkeOmfattetAvNorskTrygd) {
        this.barnOmfattetAvNorskTrygd = barnOmfattetAvNorskTrygd;
        this.barnIkkeOmfattetAvNorskTrygd = barnIkkeOmfattetAvNorskTrygd;
    }
}
