package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collection;

public record MottakerFeilmeldingDto(String tittel, Collection<FeilmeldingUnderpunkter> underpunkter) {
}

