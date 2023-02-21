package no.nav.melosys.domain.brev.utkast;

import no.nav.melosys.domain.kodeverk.Mottakerroller;

public record KopiMottakerUtkast(
    Mottakerroller rolle,
    String orgnr,
    String aktørID,
    String institusjonID
) {
}

