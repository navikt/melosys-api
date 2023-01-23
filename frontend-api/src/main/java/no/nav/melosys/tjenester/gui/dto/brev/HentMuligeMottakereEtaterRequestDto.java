package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public record HentMuligeMottakereEtaterRequestDto(Produserbaredokumenter produserbartdokument,
                                                  List<String> orgnrEtater) {
}
