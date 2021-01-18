package no.nav.melosys.domain.familie;

import java.util.Set;

public class AvklarteMedfolgendeEktefelleSamboer {
    public final Set<OmfattetFamilie> ektefelleSamboerOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetEktefelleSamboer> ektefelleSamboerIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeEktefelleSamboer(Set<OmfattetFamilie> ektefelleSamboerOmfattetAvNorskTrygd,
        Set<IkkeOmfattetEktefelleSamboer> ektefelleSamboerIkkeOmfattetAvNorskTrygd) {
        this.ektefelleSamboerOmfattetAvNorskTrygd = ektefelleSamboerOmfattetAvNorskTrygd;
        this.ektefelleSamboerIkkeOmfattetAvNorskTrygd = ektefelleSamboerIkkeOmfattetAvNorskTrygd;
    }
}
