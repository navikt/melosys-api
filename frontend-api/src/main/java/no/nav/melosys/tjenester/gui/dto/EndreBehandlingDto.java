package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

import java.time.LocalDate;

public record EndreBehandlingDto(
    Sakstyper sakstype,
    Behandlingstyper behandlingstype,
    Behandlingstema behandlingstema,
    Behandlingsstatus behandlingsstatus,
    LocalDate behandlingsfrist
) {
}
