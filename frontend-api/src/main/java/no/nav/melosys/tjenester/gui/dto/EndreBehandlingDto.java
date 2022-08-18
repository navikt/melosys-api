package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public record EndreBehandlingDto(
    Sakstyper sakstype,
    Behandlingstyper behandlingstype,
    Behandlingstema behandlingstema,
    Behandlingsstatus behandlingsstatus,
    LocalDate behandlingsfrist,
    Sakstemaer sakstema
) {
}
