package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VurderJournalfoeringstypeTest {

    private VurderJournalfoeringstype agent;

    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_BEHANDLING = "MELTEST-2";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        agent = new VurderJournalfoeringstype(fagsakRepository);

        Fagsak fagsak = new Fagsak();

        Fagsak fagsakMedBehandling = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsakMedBehandling.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsak);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_BEHANDLING)).thenReturn(fagsakMedBehandling);
    }

    @Test
    public void ukjentProsesstype_Feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        assertThat(p.getHendelser()).isNotEmpty();
        assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Ukjent prosesstype: " + p.getType());
    }

    @Test
    public void nySak_tilHentAktørID() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }

    @Test
    public void knyttTilFagsakMedAktivBehandling_tilOppdaterJournalpost() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }

    @Test
    public void knyttMedBehandlingstypeNull_tilOppdaterJournalpost() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }

    @Test
    public void knyttTilFagsakUtenAktivBehandling_tilNyBehandling() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        agent.utførSteg(p);
        assertThat(p.getType()).isEqualTo(ProsessType.JFR_NY_BEHANDLING);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }
}
