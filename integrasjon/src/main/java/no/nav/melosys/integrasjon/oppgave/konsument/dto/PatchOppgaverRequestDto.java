package no.nav.melosys.integrasjon.oppgave.konsument.dto;

import java.util.List;

public record PatchOppgaverRequestDto(String status, List<PatchOppgaveDto> oppgaver) {
}
