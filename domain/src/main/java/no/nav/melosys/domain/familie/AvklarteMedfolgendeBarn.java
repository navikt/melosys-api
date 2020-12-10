package no.nav.melosys.domain.familie;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AvklarteMedfolgendeBarn {
    public final Set<OmfattetBarn> barnOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeBarn(Set<OmfattetBarn> barnOmfattetAvNorskTrygd,
                                   Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
        this.barnOmfattetAvNorskTrygd = barnOmfattetAvNorskTrygd;
        this.barnIkkeOmfattetAvNorskTrygd = barnIkkeOmfattetAvNorskTrygd;
    }

    public Optional<String> hentBegrunnelseFritekst() {
        return barnIkkeOmfattetAvNorskTrygd.stream()
            .map(IkkeOmfattetBarn::getBegrunnnelseFritekst)
            .filter(Objects::nonNull)
            .findFirst();
    }

    public boolean finnes() {
        return !(barnOmfattetAvNorskTrygd.isEmpty() && barnIkkeOmfattetAvNorskTrygd.isEmpty());
    }
}
