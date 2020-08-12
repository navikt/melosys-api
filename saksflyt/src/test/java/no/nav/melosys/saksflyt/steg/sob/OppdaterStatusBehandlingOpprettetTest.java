package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sob.SobService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_HENT_REGISTER_OPPL;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_OPPDATER_SAKSRELASJON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterStatusBehandlingOpprettetTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SobService sobService;

    private OppdaterStatusBehandlingOpprettet oppdaterStatusBehandlingOpprettet;

    @Before
    public void setUp() {
        oppdaterStatusBehandlingOpprettet = new OppdaterStatusBehandlingOpprettet(sobService);
    }

    @Test
    public void utfør_alltid_kallerSakOgBehandling() throws TekniskException, FunksjonellException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling b = lagBehandling();
        prosessinstans.setBehandling(b);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123");
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "123");

        oppdaterStatusBehandlingOpprettet.utfør(prosessinstans);

        verify(sobService).sakOgBehandlingOpprettet(eq("123"), eq(123L), eq("123"));
        assertThat(prosessinstans.getSteg()).isEqualTo(JFR_OPPDATER_SAKSRELASJON);
    }

    @Test
    public void utfør_typeNySakFraDok_tilHentSaksopplysninger() throws TekniskException, FunksjonellException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK);
        Behandling b = lagBehandling();
        prosessinstans.setBehandling(b);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123");
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, "123");

        oppdaterStatusBehandlingOpprettet.utfør(prosessinstans);

        verify(sobService).sakOgBehandlingOpprettet(eq("123"), eq(123L), eq("123"));
        assertThat(prosessinstans.getSteg()).isEqualTo(JFR_HENT_REGISTER_OPPL);
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setId(123L);
        return behandling;
    }
}