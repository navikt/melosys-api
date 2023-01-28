package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.service.brev.feature.hentmuligebrevmottakere.HentMuligeBrevmottakereResponseDto;

public record HentMuligeBrevmottakereResponse(MuligBrevmottaker hovedMottaker,
                                              List<MuligBrevmottaker> kopiMottakere,
                                              List<MuligBrevmottaker> fasteMottakere) {
    public static HentMuligeBrevmottakereResponse byggFraHentMottakerResponse(HentMuligeBrevmottakereResponseDto hentMottakerResponse) {
        var hovedMottaker = MuligBrevmottaker.byggFraBrevmottakerDto(hentMottakerResponse.hovedMottaker());
        var kopiMottakere = hentMottakerResponse.kopiMottakere().stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
        var fasteMottakere = hentMottakerResponse.fasteMottakere().stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
        return new HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere);
    }
}
