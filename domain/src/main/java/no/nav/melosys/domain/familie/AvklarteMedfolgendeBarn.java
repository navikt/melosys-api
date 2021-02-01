package no.nav.melosys.domain.familie;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AvklarteMedfolgendeBarn {
    public final Set<OmfattetFamilie> barnOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeBarn(Set<OmfattetFamilie> barnOmfattetAvNorskTrygd,
                                   Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
        this.barnOmfattetAvNorskTrygd = barnOmfattetAvNorskTrygd;
        this.barnIkkeOmfattetAvNorskTrygd = barnIkkeOmfattetAvNorskTrygd;
    }

    public Optional<String> hentBegrunnelseFritekst() {
        return barnIkkeOmfattetAvNorskTrygd.stream()
            .map(IkkeOmfattetBarn::getBegrunnelseFritekst)
            .filter(Objects::nonNull)
            .findFirst();
    }

    public boolean finnes() {
        return !(barnOmfattetAvNorskTrygd.isEmpty() && barnIkkeOmfattetAvNorskTrygd.isEmpty());
    }
}
