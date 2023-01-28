package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.feature.hentmuligebrevmottakere.HentMuligeBrevmottakereRequestDto;

public record HentMuligeBrevmottakereRequest(Produserbaredokumenter produserbartdokument, String orgnr) {
    public HentMuligeBrevmottakereRequestDto tilHentMottakereRequest(Long behandlingID) {
        return new HentMuligeBrevmottakereRequestDto(this.produserbartdokument(), behandlingID, this.orgnr());
    }
}
