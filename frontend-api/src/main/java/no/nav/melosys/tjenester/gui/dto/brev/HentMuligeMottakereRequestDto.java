package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public record HentMuligeMottakereRequestDto(Produserbaredokumenter produserbartdokument, String orgnr) {}
