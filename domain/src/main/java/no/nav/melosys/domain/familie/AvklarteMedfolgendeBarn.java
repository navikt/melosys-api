package no.nav.melosys.domain.familie;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class AvklarteMedfolgendeBarn {

    static final Pattern UUID_V4_PATTERN
        = Pattern.compile("^[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}$");

    public final Set<OmfattetBarn> barnOmfattetAvNorskTrygd;
    public final Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeBarn(Set<OmfattetBarn> barnOmfattetAvNorskTrygd, Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd) {
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
