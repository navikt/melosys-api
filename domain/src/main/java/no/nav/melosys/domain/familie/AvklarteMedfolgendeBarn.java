package no.nav.melosys.domain.familie;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AvklarteMedfolgendeBarn {

    public final Set<String> barnOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeBarn(Set<String> barnOmfattetAvNorskTrygd, Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
        this.barnOmfattetAvNorskTrygd = barnOmfattetAvNorskTrygd;
        this.barnIkkeOmfattetAvNorskTrygd = barnIkkeOmfattetAvNorskTrygd;
    }

    public Optional<String> hentBegrunnelseFritekst() {
        return barnIkkeOmfattetAvNorskTrygd.stream()
            .map(IkkeOmfattetBarn::getBegrunnnelseFritekst)
            .filter(Objects::nonNull)
            .findFirst();
    }
}
