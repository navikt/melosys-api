package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.brev.utkast.KopiMottakerUtkast;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.saksflytapi.journalfoering.KopiMottaker;

public record KopiMottakerDto(Mottakerroller rolle,
                              String orgnr,
                              String aktørId,
                              String institusjonID) {
    public static KopiMottakerDto av(KopiMottakerUtkast kopiMottakerUtkast) {
        return new KopiMottakerDto(kopiMottakerUtkast.rolle(), kopiMottakerUtkast.orgnr(), kopiMottakerUtkast.aktørID(), kopiMottakerUtkast.institusjonID());
    }

    public KopiMottakerUtkast tilUtkast() {
        return new KopiMottakerUtkast(this.rolle(), this.orgnr(), this.aktørId(), this.institusjonID());
    }

    public KopiMottaker tilKopiMottaker() {
        return new KopiMottaker(rolle, orgnr, aktørId, institusjonID);
    }
}
