package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.brev.utkast.KopiMottakerUtkast;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record KopiMottaker(Aktoersroller rolle,
                           String orgnr,
                           String aktørId,
                           String institusjonId) {
    public static KopiMottaker av(KopiMottakerUtkast kopiMottakerUtkast) {
        return new KopiMottaker(kopiMottakerUtkast.rolle(), kopiMottakerUtkast.orgnr(), kopiMottakerUtkast.aktørID(), kopiMottakerUtkast.institusjonID());
    }

    public KopiMottakerUtkast tilUtkast() {
        return new KopiMottakerUtkast(this.rolle(), this.orgnr(), this.aktørId(), this.institusjonId());
    }
}
