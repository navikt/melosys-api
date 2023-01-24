package no.nav.melosys.service.brev.muligemottakere.hentmottakere;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public record HentMottakereRequest(Produserbaredokumenter produserbartdokument, long behandlingID, String orgnr) {
}
