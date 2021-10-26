package no.nav.melosys.domain.brev.storbritannia;

import java.time.Instant;
import java.util.List;

public record Arbeidstaker(String navn, Instant foedselsdato, String fnr, List<String> bostedsadresse) {
}
