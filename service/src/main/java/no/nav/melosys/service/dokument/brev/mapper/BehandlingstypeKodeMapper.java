package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode;
import no.nav.melosys.domain.Behandling;

final class BehandlingstypeKodeMapper {

    private BehandlingstypeKodeMapper() {
        throw new IllegalStateException("Utility class");
    }

    static BehandlingstypeKode hentBehandlingstypeKode(Behandling behandling) {

        if (behandling.erBehandlingAvSøknadGammel()) {
            if (behandling.erNyVurdering()) {
                return BehandlingstypeKode.NY_VURDERING;
            } else if (behandling.erEndretPeriode()) {
                return BehandlingstypeKode.ENDRET_PERIODE;
            } else if (behandling.erKlage()) {
                return BehandlingstypeKode.KLAGE;
            }
            return BehandlingstypeKode.SOEKNAD;
        } else if (behandling.erNorgeUtpekt()) {
            return BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE;
        }

        return BehandlingstypeKode.valueOf(behandling.getType().getKode());
    }
}
