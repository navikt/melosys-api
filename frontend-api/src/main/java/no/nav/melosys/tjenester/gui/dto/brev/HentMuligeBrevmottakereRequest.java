package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public record HentMuligeBrevmottakereRequest(Produserbaredokumenter produserbartdokument, String orgnr) {
    public no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereRequestDto tilHentMottakereRequest(Long behandlingID) {
        return new no.nav.melosys.service.brev.hentmuligemottakere.HentMuligeBrevmottakereRequestDto(this.produserbartdokument(), behandlingID, this.orgnr());
    }
}
