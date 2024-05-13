package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;

public record MuligBrevmottaker(String mottakerNavn,
                                String dokumentNavn,
                                Mottakerroller rolle,
                                String orgnr,
                                String aktørId,
                                String institusjonID) {
    public static MuligBrevmottaker av(Brevmottaker hovedMottaker) {
        return new MuligBrevmottaker(
            hovedMottaker.getMottakerNavn(),
            hovedMottaker.getDokumentNavn(),
            hovedMottaker.getRolle(),
            hovedMottaker.getOrgnr(),
            hovedMottaker.getAktørId(),
            hovedMottaker.getInstitusjonID());
    }
}
