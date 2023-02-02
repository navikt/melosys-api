package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.bestilling.HentMuligeBrevmottakereService;

public record HentMuligeBrevmottakereRequest(Produserbaredokumenter produserbartdokument, String orgnr) {
    public HentMuligeBrevmottakereService.RequestDto tilHentMuligeBrevmottakereRequestDto(Long behandlingID) {
        return new HentMuligeBrevmottakereService.RequestDto(this.produserbartdokument(), behandlingID, this.orgnr());
    }
}
