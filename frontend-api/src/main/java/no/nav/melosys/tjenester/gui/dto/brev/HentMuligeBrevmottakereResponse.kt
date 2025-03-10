package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.service.brev.bestilling.HentMuligeBrevmottakereService;

public record HentMuligeBrevmottakereResponse(MuligBrevmottaker hovedMottaker,
                                              List<MuligBrevmottaker> kopiMottakere,
                                              List<MuligBrevmottaker> fasteMottakere) {
    public static HentMuligeBrevmottakereResponse av(HentMuligeBrevmottakereService.ResponseDto hentMottakerResponse) {
        var hovedMottaker = MuligBrevmottaker.av(hentMottakerResponse.hovedMottaker());
        var kopiMottakere = hentMottakerResponse.kopiMottakere().stream().map(MuligBrevmottaker::av).toList();
        var fasteMottakere = hentMottakerResponse.fasteMottakere().stream().map(MuligBrevmottaker::av).toList();
        return new HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere);
    }
}
