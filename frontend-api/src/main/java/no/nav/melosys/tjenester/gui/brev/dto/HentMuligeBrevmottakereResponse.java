package no.nav.melosys.tjenester.gui.brev.dto;

import java.util.List;

public record HentMuligeBrevmottakereResponse(MuligBrevmottakerResponse hovedMottaker,
                                              List<MuligBrevmottakerResponse> kopiMottakere,
                                              List<MuligBrevmottakerResponse> fasteMottakere) {
    public static HentMuligeBrevmottakereResponse byggFraHentMottakerResponse(no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereResponseDto hentMottakerResponse) {
        var hovedMottaker = MuligBrevmottakerResponse.byggFraBrevmottakerDto(hentMottakerResponse.hovedMottaker());
        var kopiMottakere = hentMottakerResponse.kopiMottakere().stream().map(MuligBrevmottakerResponse::byggFraBrevmottakerDto).toList();
        var fasteMottakere = hentMottakerResponse.fasteMottakere().stream().map(MuligBrevmottakerResponse::byggFraBrevmottakerDto).toList();
        return new HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere);
    }
}
