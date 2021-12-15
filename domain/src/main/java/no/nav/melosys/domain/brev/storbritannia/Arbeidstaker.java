package no.nav.melosys.domain.brev.storbritannia;

import java.time.LocalDate;
import java.util.List;

public record Arbeidstaker(String navn, LocalDate foedselsdato, String fnr, List<String> bostedsadresse) {
}
