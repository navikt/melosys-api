package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.medl.MedlAnmodningsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AvsluttTidligereMedlAnmodningsperiodeTest {
    @Mock
    private MedlAnmodningsperiodeService medlAnmodningsperiodeService;

    private AvsluttTidligereMedlAnmodningsperiode avsluttTidligereMedlAnmodningsperiode;
    private Behandling behandling;
    private Prosessinstans prosessinstans;

    @BeforeEach
    public void setUp() {
        avsluttTidligereMedlAnmodningsperiode = new AvsluttTidligereMedlAnmodningsperiode(medlAnmodningsperiodeService);
        prosessinstans = Prosessinstans.builder().medType(ProsessType.OPPRETT_SAK).medStatus(ProsessStatus.KLAR).build();
    }

    @Test
    void utfør_mottarOppdatertA001_kallerPå_avsluttTidligereAnmodningsperiode() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            .build();
        prosessinstans = prosessinstans.toBuilder()
            .medBehandling(behandling)
            .medData(ProsessDataKey.ER_OPPDATERT_SED, true)
            .build();

        avsluttTidligereMedlAnmodningsperiode.utfør(prosessinstans);

        verify(medlAnmodningsperiodeService).avsluttTidligereAnmodningsperiode(behandling);
    }

    @Test
    void utfør_mottarNyA001_kallerIkkePå_avsluttTidligereAnmodningsperiode() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            .build();
        prosessinstans = prosessinstans.toBuilder()
            .medBehandling(behandling)
            .medData(ProsessDataKey.ER_OPPDATERT_SED, false)
            .build();

        avsluttTidligereMedlAnmodningsperiode.utfør(prosessinstans);

        verify(medlAnmodningsperiodeService, never()).avsluttTidligereAnmodningsperiode(behandling);
    }

    @Test
    void utfør_mottarOppdatertA009_kallerIkkePå_avsluttTidligereAnmodningsperiode() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING)
            .build();
        prosessinstans = prosessinstans.toBuilder()
            .medBehandling(behandling)
            .medData(ProsessDataKey.ER_OPPDATERT_SED, true)
            .build();

        avsluttTidligereMedlAnmodningsperiode.utfør(prosessinstans);

        verify(medlAnmodningsperiodeService, never()).avsluttTidligereAnmodningsperiode(behandling);
    }
}
