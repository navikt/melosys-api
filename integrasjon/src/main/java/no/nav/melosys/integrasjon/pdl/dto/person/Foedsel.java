package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Foedsel(LocalDate foedselsdato,
                      Integer foedselsaar,
                      String foedeland,
                      String foedested,
                      Metadata metadata) {
}
