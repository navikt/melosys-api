package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public record VurderUnntaksperiodeDto(Set<String> ikkeGodkjentBegrunnelseKoder, String begrunnelseFritekst) { }
