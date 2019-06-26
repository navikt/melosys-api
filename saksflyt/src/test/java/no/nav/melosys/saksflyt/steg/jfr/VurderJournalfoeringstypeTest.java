package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Arrays;
import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
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
    private final static String SAKSNUMMER_MED_AKTIV_BEHANDLING = "MELTEST-2";
    private final static String SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING = "MELTEST-4";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        agent = new VurderJournalfoeringstype(fagsakRepository);

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(Collections.emptyList());

        Fagsak fagsakMedAktivBehandling = new Fagsak();
        Fagsak fagsakMedInaktivBehandling = new Fagsak();
        Fagsak fagsakMedInaktivOgAktivBehandling = new Fagsak();
        Behandling aktivBehandling = new Behandling();
        aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Behandling inaktivBehandling = new Behandling();
        inaktivBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        fagsakMedAktivBehandling.setBehandlinger(Collections.singletonList(aktivBehandling));
        fagsakMedInaktivBehandling.setBehandlinger(Collections.singletonList(inaktivBehandling));
        fagsakMedInaktivOgAktivBehandling.setBehandlinger(Arrays.asList(aktivBehandling, inaktivBehandling));

        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsak);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_AKTIV_BEHANDLING)).thenReturn(fagsakMedAktivBehandling);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING)).thenReturn(fagsakMedInaktivBehandling);
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
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
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
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        agent.utførSteg(p);
        assertThat(p.getType()).isEqualTo(ProsessType.JFR_NY_BEHANDLING);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedInaktivOgUtenAktivBehandling_steg_jfrOppdaterJournalpost() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_AKTIV_BEHANDLING_OG_MED_INAKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }

    @Test
    public void knyttTilFagsakMedEndretPeriodeMedAktivBehandling_kasterException() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_AKTIV_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }
}
