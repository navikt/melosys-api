package no.nav.melosys.integrasjon.oppgave.konsument.dto;

import java.util.List;
import java.util.Map;

public record PatchOppgaverResponseDto(Long feilet, Long suksess, Long totalt, Map<Integer, List<Long>> data) {
}
