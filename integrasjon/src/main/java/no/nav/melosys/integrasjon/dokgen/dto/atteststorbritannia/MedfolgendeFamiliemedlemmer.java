package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.util.List;
import java.util.stream.Collectors;

public record MedfolgendeFamiliemedlemmer(MedfolgendeFamiliemedlem ektefelle, List<MedfolgendeFamiliemedlem> barn) {

    public static MedfolgendeFamiliemedlemmer av(no.nav.melosys.domain.brev.storbritannia.MedfolgendeFamiliemedlemmer familiemedlemmer) {
        if (familiemedlemmer == null) return null;

        return new MedfolgendeFamiliemedlemmer(
            MedfolgendeFamiliemedlem.av(familiemedlemmer.ektefelle()),
            familiemedlemmer.barn().stream().map(MedfolgendeFamiliemedlem::av).collect(Collectors.toList())
        );
    }
}
