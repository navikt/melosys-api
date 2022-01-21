package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record KopiMottaker(Aktoersroller rolle,
                           String orgnr,
                           String aktørId,
                           String institusjonId) {
}
