package no.nav.melosys.domain.brev.storbritannia;

import java.util.Set;

public record MedfolgendeFamiliemedlemmer(MedfolgendeFamiliemedlem ektefelle, Set<MedfolgendeFamiliemedlem> barn) {
}
