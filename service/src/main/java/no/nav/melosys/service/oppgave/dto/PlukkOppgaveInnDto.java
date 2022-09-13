package no.nav.melosys.service.oppgave.dto;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public record PlukkOppgaveInnDto(
    Behandlingstema behandlingstema,
    Sakstemaer sakstema,
    Sakstyper sakstype
) {

}
