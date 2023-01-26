package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.muligemottakere.hentmottakere.HentMottakereRequest;

public record HentMuligeMottakereRequestDto(Produserbaredokumenter produserbartdokument, String orgnr) {
    public HentMottakereRequest tilHentMottakereRequest(Long behandlingID) {
        return new HentMottakereRequest(this.produserbartdokument(), behandlingID, this.orgnr());
    }
}
