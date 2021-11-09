package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.util.List;

public record Familie(
    boolean minstEttOmfattetFamiliemedlem,
    Ektefelle ektefelle,
    List<Barn> barn
) {
}
