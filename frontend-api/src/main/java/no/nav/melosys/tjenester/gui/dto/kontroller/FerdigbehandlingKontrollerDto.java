package no.nav.melosys.tjenester.gui.dto.kontroller;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public record FerdigbehandlingKontrollerDto(
    long behandlingID,
    Vedtakstyper vedtakstype,
    Behandlingsresultattyper behandlingsresultattype,
    Set<Kontroll_begrunnelser> kontrollerSomSkalIgnoreres,
    boolean skalRegisteropplysningerOppdateres
) {
}
