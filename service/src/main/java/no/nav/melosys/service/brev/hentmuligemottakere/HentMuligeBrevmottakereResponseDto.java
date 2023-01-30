package no.nav.melosys.service.brev.hentmuligemottakere;

import java.util.List;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;

public record HentMuligeBrevmottakereResponseDto(Brevmottaker hovedMottaker,
                                                 List<Brevmottaker> kopiMottakere,
                                                 List<Brevmottaker> fasteMottakere) {
}
