package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public record IkkeGodkjennUnntaksperiodeDto(Set<String> ikkeGodkjentBegrunnelseKoder, String begrunnelseFritekst) { }

