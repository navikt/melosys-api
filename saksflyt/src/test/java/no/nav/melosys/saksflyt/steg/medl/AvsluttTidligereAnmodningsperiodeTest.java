package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AvsluttTidligereAnmodningsperiodeTest {
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private AvsluttTidligereAnmodningsperiode avsluttTidligereAnmodningsperiode;
    private Behandling behandling;
    private Prosessinstans prosessinstans;

    @BeforeEach
    public void setUp() {
        avsluttTidligereAnmodningsperiode = new AvsluttTidligereAnmodningsperiode(medlPeriodeService);
        behandling = new Behandling();
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
    }

    @Test
    void utfør_mottarOppdatertA001_verifiserAvsluttTidligereAnmodningsperiode() {
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, true);

        avsluttTidligereAnmodningsperiode.utfør(prosessinstans);

        verify(medlPeriodeService).avsluttTidligereAnmodningsperiode(behandling);
    }

    @Test
    void utfør_mottarNyA001_verifiserIKKEAvsluttTidligereAnmodningsperiode() {
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, false);

        avsluttTidligereAnmodningsperiode.utfør(prosessinstans);

        verify(medlPeriodeService, never()).avsluttTidligereAnmodningsperiode(behandling);
    }

    @Test
    void utfør_mottarOppdatertA009_verifiserIKKEAvsluttTidligereAnmodningsperiode() {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, true);

        avsluttTidligereAnmodningsperiode.utfør(prosessinstans);

        verify(medlPeriodeService, never()).avsluttTidligereAnmodningsperiode(behandling);
    }
}
