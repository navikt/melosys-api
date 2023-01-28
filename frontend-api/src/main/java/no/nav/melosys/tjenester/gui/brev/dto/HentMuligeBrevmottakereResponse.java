package no.nav.melosys.tjenester.gui.brev.dto;

import java.util.List;

public record HentMuligeBrevmottakereResponse(MuligBrevmottaker hovedMottaker,
                                              List<MuligBrevmottaker> kopiMottakere,
                                              List<MuligBrevmottaker> fasteMottakere) {
    public static HentMuligeBrevmottakereResponse byggFraHentMottakerResponse(no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereResponseDto hentMottakerResponse) {
        var hovedMottaker = MuligBrevmottaker.byggFraBrevmottakerDto(hentMottakerResponse.hovedMottaker());
        var kopiMottakere = hentMottakerResponse.kopiMottakere().stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
        var fasteMottakere = hentMottakerResponse.fasteMottakere().stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
        return new HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere);
    }
}
