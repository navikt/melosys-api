package no.nav.melosys.service.brev.muligemottakere.hentmottakere;

import java.util.List;

import no.nav.melosys.service.brev.muligemottakere.MuligMottakerDto;

public record HentMottakereResponse(MuligMottakerDto hovedMottaker, List<MuligMottakerDto> kopiMottakere,
                                    List<MuligMottakerDto> fasteMottakere) {
}
