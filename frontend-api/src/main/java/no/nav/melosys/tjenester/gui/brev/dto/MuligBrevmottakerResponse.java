package no.nav.melosys.tjenester.gui.brev.dto;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record MuligBrevmottakerResponse(String mottakerNavn,
                                        String dokumentNavn,
                                        Aktoersroller rolle,
                                        String orgnr,
                                        String aktørId,
                                        String institusjonId) {
    public static MuligBrevmottakerResponse byggFraBrevmottakerDto(Brevmottaker hovedMottaker) {
        return new MuligBrevmottakerResponse(
            hovedMottaker.getMottakerNavn(),
            hovedMottaker.getDokumentNavn(),
            hovedMottaker.getRolle(),
            hovedMottaker.getOrgnr(),
            hovedMottaker.getAktørId(),
            hovedMottaker.getInstitusjonId());
    }
}
