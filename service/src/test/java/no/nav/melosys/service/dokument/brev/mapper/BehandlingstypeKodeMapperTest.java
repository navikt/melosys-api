package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.dokument.brev.mapper.BehandlingstypeKodeMapper.hentBehandlingstypeKode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class BehandlingstypeKodeMapperTest {

    @Test
    void hentBehandlingstypeKodeAlleBehandlinger_mapKorrektBehandlingstype() {
        assertThat(hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.SOEKNAD);
        assertThat(hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_SELVSTENDIG))).isEqualTo(BehandlingstypeKode.SOEKNAD);
        assertThat(hentBehandlingstypeKode(behandling(ENDRET_PERIODE, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.ENDRET_PERIODE);
        assertThat(hentBehandlingstypeKode(behandling(NY_VURDERING, UTSENDT_SELVSTENDIG))).isEqualTo(BehandlingstypeKode.NY_VURDERING);
        assertThat(hentBehandlingstypeKode(behandling(KLAGE, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.KLAGE);
        assertThat(hentBehandlingstypeKode(behandling(FØRSTEGANG, BESLUTNING_LOVVALG_NORGE))).isEqualTo(BehandlingstypeKode.UTL_MYND_UTPEKT_NORGE);
        assertThat(hentBehandlingstypeKode(behandling(FØRSTEGANG, UTSENDT_ARBEIDSTAKER))).isEqualTo(BehandlingstypeKode.SOEKNAD);
    }

    @Test
    void hentBehandlingstypeKodeAlleBehandlinger_BehandlingstypeHENVENDELSE_kasterException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> hentBehandlingstypeKode(behandling(HENVENDELSE, UTSENDT_ARBEIDSTAKER)))
            .withMessageContaining("Støtter ikke behandling med type : HENVENDELSE");
    }

    private Behandling behandling(Behandlingstyper behandlingstype, Behandlingstema behandlingstema) {
        Behandling behandling = new Behandling();
        behandling.setTema(behandlingstema);
        behandling.setType(behandlingstype);
        return behandling;
    }
}
