package no.nav.melosys.tjenester.gui.brev.dto;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record MuligBrevmottaker(String mottakerNavn,
                                String dokumentNavn,
                                Aktoersroller rolle,
                                String orgnr,
                                String aktørId,
                                String institusjonId) {
    public static MuligBrevmottaker byggFraBrevmottakerDto(Brevmottaker hovedMottaker) {
        return new MuligBrevmottaker(
            hovedMottaker.getMottakerNavn(),
            hovedMottaker.getDokumentNavn(),
            hovedMottaker.getRolle(),
            hovedMottaker.getOrgnr(),
            hovedMottaker.getAktørId(),
            hovedMottaker.getInstitusjonId());
    }
}
