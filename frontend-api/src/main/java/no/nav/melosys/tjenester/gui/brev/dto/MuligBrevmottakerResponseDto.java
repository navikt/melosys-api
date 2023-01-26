package no.nav.melosys.tjenester.gui.brev.dto;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;

public record MuligBrevmottakerResponseDto(String mottakerNavn,
                                           String dokumentNavn,
                                           Aktoersroller rolle,
                                           String orgnr,
                                           String aktørId,
                                           String institusjonId) {
    public static MuligBrevmottakerResponseDto byggFraBrevmottakerDto(Brevmottaker hovedMottaker) {
        return new MuligBrevmottakerResponseDto(
            hovedMottaker.getMottakerNavn(),
            hovedMottaker.getDokumentNavn(),
            hovedMottaker.getRolle(),
            hovedMottaker.getOrgnr(),
            hovedMottaker.getAktørId(),
            hovedMottaker.getInstitusjonId());
    }
}
