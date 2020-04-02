package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.KontrollresultatService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegisterKontrollTest {

    private RegisterKontroll registerKontroll;

    @Mock
    private KontrollresultatService kontrollresultatService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Before
    public void setup() {
        registerKontroll = new RegisterKontroll(kontrollresultatService, behandlingsresultatService);
    }

    @Test
    public void utfør_ingenTreffIKontrollNorgeIkkeUtpekt_nesteStegOppdaterMedl() throws MelosysException {
        Prosessinstans prosessinstans = prosessinstans(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat(false));
        registerKontroll.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AFL_OPPDATER_MEDL);
    }

    @Test
    public void utfør_erTreffIKontrollNorgeIkkeUtpekt_nesteStegOpprettOppgave() throws MelosysException {
        Prosessinstans prosessinstans = prosessinstans(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat(true));
        registerKontroll.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AFL_OPPRETT_OPPGAVE);
    }

    @Test
    public void utfør_ingenTreffIKontrollNorgeErUtpekt_nesteStegOpprettOppgave() throws MelosysException {
        Prosessinstans prosessinstans = prosessinstans(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat(false));
        registerKontroll.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AFL_OPPRETT_OPPGAVE);
    }

    private Behandlingsresultat behandlingsresultat(boolean medKontroller) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        if (medKontroller) {
            Kontrollresultat kontrollresultat = new Kontrollresultat();
            kontrollresultat.setBegrunnelse(Kontroll_begrunnelser.FEIL_I_PERIODEN);
            behandlingsresultat.getKontrollresultater().add(kontrollresultat);
        }

        return behandlingsresultat;
    }

    private Prosessinstans prosessinstans(Behandlingstema behandlingstema) {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setTema(behandlingstema);
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}
