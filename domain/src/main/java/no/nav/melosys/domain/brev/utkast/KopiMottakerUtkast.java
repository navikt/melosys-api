package no.nav.melosys.domain.brev.utkast;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record KopiMottakerUtkast(
    Aktoersroller rolle,
    String orgnr,
    String aktørID,
    String institusjonID
) {
}

