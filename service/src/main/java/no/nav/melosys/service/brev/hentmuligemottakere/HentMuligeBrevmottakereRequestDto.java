package no.nav.melosys.service.brev.hentmuligemottakere;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public record HentMuligeBrevmottakereRequestDto(Produserbaredokumenter produserbartdokument,
                                                long behandlingID,
                                                String orgnr) {
}
