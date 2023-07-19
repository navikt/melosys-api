package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collection;

public record FeilmeldingDto(String tittel, Collection<FeilmeldingUnderpunkt> underpunkter) {
}

