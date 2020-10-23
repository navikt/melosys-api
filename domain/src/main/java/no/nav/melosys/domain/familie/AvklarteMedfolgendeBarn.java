package no.nav.melosys.domain.familie;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AvklarteMedfolgendeBarn {

    public final Set<String> medfølgendeBarn;
    public final Set<AvklartIkkeMedfolgendeBarn> ikkeMedfølgendeBarn;

    public AvklarteMedfolgendeBarn(Set<String> medfølgendeBarn, Set<AvklartIkkeMedfolgendeBarn> ikkeMedfølgendeBarn) {
        this.medfølgendeBarn = medfølgendeBarn;
        this.ikkeMedfølgendeBarn = ikkeMedfølgendeBarn;
    }

    public Optional<String> begrunnelseFritekst() {
        return ikkeMedfølgendeBarn.stream()
            .map(AvklartIkkeMedfolgendeBarn::getBegrunnnelseFritekst)
            .filter(Objects::nonNull)
            .findFirst();
    }
}
