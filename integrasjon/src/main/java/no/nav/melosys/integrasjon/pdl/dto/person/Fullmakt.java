package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;
import java.util.Collection;

public record Fullmakt(
    String motpartsPersonident,
    FullmaktsRolle motpartsRolle,
    Collection<String> omraader,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed
) {
}
