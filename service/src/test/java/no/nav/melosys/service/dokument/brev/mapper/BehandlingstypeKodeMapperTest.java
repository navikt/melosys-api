package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.dokument.brev.mapper.BehandlingstypeKodeMapper.hentBehandlingstypeKode;
import static no.nav.melosys.service.dokument.brev.mapper.BehandlingstypeKodeMapper.hentBehandlingstypeKodeAlleBehandlinger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class BehandlingstypeKodeMapperTest {

    @Test
    public void hentBehandlingstypeKode_mapKorrektBehandlingstype() {
        assertThat(hentBehandlingstypeKode(behandling(SOEKNAD, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.SOEKNAD);
        assertThat(hentBehandlingstypeKode(behandling(SOEKNAD, UTSENDT_SELVSTENDIG))).isEqualTo(BehandlingstypeKode.SOEKNAD);
        assertThat(hentBehandlingstypeKode(behandling(ENDRET_PERIODE, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.ENDRET_PERIODE);
        assertThat(hentBehandlingstypeKode(behandling(NY_VURDERING, UTSENDT_SELVSTENDIG))).isEqualTo(BehandlingstypeKode.NY_VURDERING);
        assertThat(hentBehandlingstypeKode(behandling(KLAGE, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.KLAGE);
        assertThat(hentBehandlingstypeKode(behandling(SED, BESLUTNING_LOVVALG_NORGE))).isEqualTo(BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE);
        assertThat(hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.SOEKNAD);
    }

    @Test
    void hentBehandlingstypeKodeAlleBehandlinger_mapKorrektBehandlingstype() {
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(SOEKNAD, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.SOEKNAD);
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(SOEKNAD, UTSENDT_SELVSTENDIG))).isEqualTo(BehandlingstypeKode.SOEKNAD);
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(ENDRET_PERIODE, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.ENDRET_PERIODE);
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(NY_VURDERING, UTSENDT_SELVSTENDIG))).isEqualTo(BehandlingstypeKode.NY_VURDERING);
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(KLAGE, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.KLAGE);
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(SED, BESLUTNING_LOVVALG_NORGE))).isEqualTo(BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE);
        assertThat(hentBehandlingstypeKodeAlleBehandlinger(behandling(FØRSTEGANG, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.SOEKNAD);
    }

    @Test
    void hentBehandlingstypeKodeAlleBehandlinger_BehandlingstypeSED_kasterException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> hentBehandlingstypeKodeAlleBehandlinger(behandling(SED, UTSENDT_ARBEIDSTAKER)))
            .withMessageContaining("Støtter ikke behandling med type : SED");
    }

    private Behandling behandling(Behandlingstyper behandlingstype, Behandlingstema behandlingstema) {
        Behandling behandling = new Behandling();
        behandling.setTema(behandlingstema);
        behandling.setType(behandlingstype);
        return behandling;
    }
}
