package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.components.HentMuligeBrevmottakereComponent;

public record HentMuligeBrevmottakereRequest(Produserbaredokumenter produserbartdokument, String orgnr) {
    public HentMuligeBrevmottakereComponent.RequestDto tilHentMuligeBrevmottakereRequestDto(Long behandlingID) {
        return new HentMuligeBrevmottakereComponent.RequestDto(this.produserbartdokument(), behandlingID, this.orgnr());
    }
}
