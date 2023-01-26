package no.nav.melosys.tjenester.gui.brev.dto;

import java.util.List;

public record HentMuligeBrevmottakereResponseDto(MuligBrevmottakerResponseDto hovedMottaker,
                                                 List<MuligBrevmottakerResponseDto> kopiMottakere,
                                                 List<MuligBrevmottakerResponseDto> fasteMottakere) {
    public static HentMuligeBrevmottakereResponseDto byggFraHentMottakerResponse(no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereResponseDto hentMottakerResponse) {
        var hovedMottaker = MuligBrevmottakerResponseDto.byggFraBrevmottakerDto(hentMottakerResponse.hovedMottaker());
        var kopiMottakere = hentMottakerResponse.kopiMottakere().stream().map(MuligBrevmottakerResponseDto::byggFraBrevmottakerDto).toList();
        var fasteMottakere = hentMottakerResponse.fasteMottakere().stream().map(MuligBrevmottakerResponseDto::byggFraBrevmottakerDto).toList();
        return new HentMuligeBrevmottakereResponseDto(hovedMottaker, kopiMottakere, fasteMottakere);
    }
}
