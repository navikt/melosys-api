package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode;
import no.nav.melosys.domain.Behandling;

final class BehandlingstypeKodeMapper {

    private BehandlingstypeKodeMapper() {
        throw new IllegalStateException("Utility class");
    }

    static BehandlingstypeKode hentBehandlingstypeKode(Behandling behandling) {

        if (behandling.erBehandlingAvSøknad()) {
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

    static BehandlingstypeKode hentBehandlingstypeKodeAlleBehandlinger(Behandling behandling) {
        if (behandling.erNorgeUtpekt()) {
            return BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE;
        } else {
            return switch (behandling.getType()) {
                case SOEKNAD, FØRSTEGANG -> BehandlingstypeKode.SOEKNAD;
                case KLAGE -> BehandlingstypeKode.KLAGE;
                case NY_VURDERING -> BehandlingstypeKode.NY_VURDERING;
                case ENDRET_PERIODE -> BehandlingstypeKode.ENDRET_PERIODE;
                default ->
                    throw new IllegalArgumentException("Støtter ikke behandling med type : " + behandling.getType());
            };
        }
    }
}
