package no.nav.melosys.tjenester.gui.dto.kontroller;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public record FerdigbehandlingKontrollerDto(
    long behandlingID,
    Vedtakstyper vedtakstype,
    Behandlingsresultattyper behandlingsresultattype,
    boolean skalRegisteropplysningerOppdateres) {
}
