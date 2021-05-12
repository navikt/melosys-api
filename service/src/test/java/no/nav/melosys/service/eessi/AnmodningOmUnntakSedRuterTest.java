package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.eessi.ruting.AnmodningOmUnntakSedRuter;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnmodningOmUnntakSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private AnmodningOmUnntakSedRuter anmodningOmUnntakSedRuter;

    private final String AKTØR_ID = "13412";
    private final Long GSAK_SAKSNUMMER = 132L;
    private final LocalDate NÅ = LocalDate.now();
    private final LocalDate NESTE_ÅR = LocalDate.now().plusYears(1);

    @BeforeEach
    public void setup() {
        anmodningOmUnntakSedRuter = new AnmodningOmUnntakSedRuter(prosessinstansService, fagsakService, behandlingsresultatService);
    }

    @Test
    public void finnSakOgBestemRuting_gsakSaksnummerErNull_NySak() throws FunksjonellException {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setAktoerId(AKTØR_ID);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(prosessinstansService).opprettProsessinstansNySakMottattAnmodningOmUnntak(eq(melosysEessiMelding), eq(melosysEessiMelding.getAktoerId()));
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererPeriodeEndret_nyBehandling() throws FunksjonellException {
        Fagsak fagsak = opprettFagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = opprettMelosysEessiMelding(NÅ, NESTE_ÅR);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(opprettBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR.plusDays(1)));
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        verify(prosessinstansService).opprettProsessinstansNyBehandlingMottattAnmodningUnntak(eq(melosysEessiMelding), eq(GSAK_SAKSNUMMER));
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererPeriodeIkkeEndret_ikkeNyBehandling() throws FunksjonellException {
        Fagsak fagsak = opprettFagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = opprettMelosysEessiMelding(NÅ, NESTE_ÅR);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(opprettBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR));
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        verify(prosessinstansService, never()).opprettProsessinstansNyBehandlingMottattAnmodningUnntak(any(), any());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(fagsak.getBehandlinger().get(0)), eq(melosysEessiMelding));
    }

    @Test
    public void finnSakOgBestemRuting_sakEksistererIkke_nySak() throws FunksjonellException {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID);

        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.empty());
        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        verify(prosessinstansService).opprettProsessinstansNySakMottattAnmodningOmUnntak(eq(melosysEessiMelding), eq(AKTØR_ID));
    }

    private Behandlingsresultat opprettBehandlingsresultatMedLovvalgsperiode(LocalDate fom, LocalDate tom) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }

    private Fagsak opprettFagsak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setRegistrertDato(Instant.now());

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }

    private MelosysEessiMelding opprettMelosysEessiMelding(LocalDate fom, LocalDate tom) {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId(AKTØR_ID);
        melding.setArtikkel("12_1");
        melding.setDokumentId("123321");
        melding.setJournalpostId("j123");
        melding.setLovvalgsland("SE");

        Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        melding.setPeriode(periode);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLandkode("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A001");
        melding.setBucType("LA_BUC_01");
        return melding;
    }
}
