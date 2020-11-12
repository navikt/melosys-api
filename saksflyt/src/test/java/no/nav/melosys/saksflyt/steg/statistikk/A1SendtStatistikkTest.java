package no.nav.melosys.saksflyt.steg.statistikk;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class A1SendtStatistikkTest {
    @Mock
    private UtstedtA1Service utstedtA1Service;
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private A1SendtStatistikk a1SendtStatistikk;

    private static final Long BEHANDLING_ID = 123L;

    @BeforeEach
    void setUp() {
        a1SendtStatistikk = new A1SendtStatistikk(utstedtA1Service, fakeUnleash);
    }

    @Test
    void utfør_featureEnabled_forventKall() throws MelosysException {
        fakeUnleash.enableAll();

        a1SendtStatistikk.utfør(lagProsessinstans());

        verify(utstedtA1Service).sendMeldingOmUtstedtA1(eq(BEHANDLING_ID));
    }

    @Test
    void utfør_featureDisabled_forventIngenKall() throws MelosysException {
        fakeUnleash.disableAll();

        a1SendtStatistikk.utfør(lagProsessinstans());

        verify(utstedtA1Service, never()).sendMeldingOmUtstedtA1(eq(BEHANDLING_ID));
    }

    private Prosessinstans lagProsessinstans() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}