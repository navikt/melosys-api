package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.eessi.ruting.AnmodningOmUnntakSedRuter;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnmodningOmUnntakSedRuterTest {

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
    void finnSakOgBestemRuting_gsakSaksnummerErNull_NySak() {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setAktoerId(AKTØR_ID);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(prosessinstansService).opprettProsessinstansNySakMottattAnmodningOmUnntak(melosysEessiMelding, melosysEessiMelding.getAktoerId());
    }

    @Test
    void finnSakOgBestemRuting_sakEksistererPeriodeEndret_nyBehandling() {
        Fagsak fagsak = opprettFagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = opprettMelosysEessiMelding(NÅ, NESTE_ÅR);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(opprettBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR.plusDays(1)));
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        verify(prosessinstansService).opprettProsessinstansNyBehandlingMottattAnmodningUnntak(melosysEessiMelding, GSAK_SAKSNUMMER);
    }

    @Test
    void finnSakOgBestemRuting_sakEksistererPeriodeIkkeEndret_ikkeNyBehandling() {
        Fagsak fagsak = opprettFagsak();
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = opprettMelosysEessiMelding(NÅ, NESTE_ÅR);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setBehandling(fagsak.getBehandlinger().get(0));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(opprettBehandlingsresultatMedLovvalgsperiode(NÅ, NESTE_ÅR));
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        verify(prosessinstansService, never()).opprettProsessinstansNyBehandlingMottattAnmodningUnntak(any(), any());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(fagsak.getBehandlinger().get(0), melosysEessiMelding);
    }

    @Test
    void finnSakOgBestemRuting_sakEksistererIkke_nySak() {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID);

        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.empty());
        anmodningOmUnntakSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        verify(prosessinstansService).opprettProsessinstansNySakMottattAnmodningOmUnntak(melosysEessiMelding, AKTØR_ID);
    }

    private Behandlingsresultat opprettBehandlingsresultatMedLovvalgsperiode(LocalDate fom, LocalDate tom) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.SE);
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }

    private Fagsak opprettFagsak() {
        Behandling behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.OPPRETTET)
            .build();

        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        behandling.setFagsak(fagsak);
        fagsak.leggTilBehandling(behandling);
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

        Statsborgerskap statsborgerskap = new Statsborgerskap("SE");

        melding.setRinaSaksnummer("r123");
        melding.setSedId("s123");
        melding.setStatsborgerskap(
            Collections.singletonList(statsborgerskap));
        melding.setSedType("A001");
        melding.setBucType("LA_BUC_01");
        return melding;
    }
}
