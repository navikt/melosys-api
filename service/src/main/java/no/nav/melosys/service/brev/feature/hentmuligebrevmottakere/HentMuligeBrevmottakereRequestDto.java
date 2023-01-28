package no.nav.melosys.service.brev.feature.hentmuligebrevmottakere;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public record HentMuligeBrevmottakereRequestDto(Produserbaredokumenter produserbartdokument,
                                                long behandlingID,
                                                String orgnr) {
}
