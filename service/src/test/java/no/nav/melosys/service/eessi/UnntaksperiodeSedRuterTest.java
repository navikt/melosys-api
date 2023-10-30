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
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.eessi.ruting.UnntaksperiodeSedRuter;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnntaksperiodeSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private UnntaksperiodeSedRuter unntaksperiodeSedRuter;

    private final String aktørID = "143455432";

    @BeforeEach
    public void setup() {
        unntaksperiodeSedRuter = new UnntaksperiodeSedRuter(prosessinstansService, fagsakService, behandlingsresultatService);
    }

    @Test
    void finnSakOgBestemRuting_nySak_verifiserResultatNySak() {
        Prosessinstans prosessinstans = hentProsessinstans(LocalDate.now(), LocalDate.now().plusYears(1));

        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, 1L);

        verify(prosessinstansService).opprettProsessinstansNySakUnntaksregistrering(
            any(MelosysEessiMelding.class), eq(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING), eq(aktørID)
        );
    }

    @Test
    void finnSakOgBestemRuting_oppdatertSedPåEksisterendeSakIkkeEndretPeriode_skalIkkeBehandles() throws Exception {

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusYears(1);
        Prosessinstans prosessinstans = hentProsessinstans(fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.SE);
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        Fagsak fagsak = hentFagsak();

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(fagsakService.finnFagsakFraArkivsakID(anyLong())).thenReturn(Optional.of(fagsak));

        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, 1L);

        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(fagsak.hentSistAktivBehandling()), any(MelosysEessiMelding.class));
    }

    @Test
    void finnSakOgBestemRuting_oppdatertSedPåEksisterendeSakErEndretPeriode_skalBehandles() throws Exception {
        final long arkivsakID = 12321L;
        LocalDate fom = LocalDate.now();
        LocalDate tom = null;
        Prosessinstans prosessinstans = hentProsessinstans(fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom.plusMonths(1));
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        Fagsak fagsak = hentFagsak();

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(fagsakService.finnFagsakFraArkivsakID(anyLong())).thenReturn(Optional.of(fagsak));

        unntaksperiodeSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(prosessinstansService).opprettProsessinstansNyBehandlingUnntaksregistrering(
            any(MelosysEessiMelding.class), eq(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING), eq(arkivsakID)
        );
    }

    private Fagsak hentFagsak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setRegistrertDato(Instant.now());

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }

    private Prosessinstans hentProsessinstans(LocalDate fom, LocalDate tom) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(fom, tom));
        return prosessinstans;
    }

    private MelosysEessiMelding hentMelosysEessiMelding(LocalDate fom, LocalDate tom) {
        MelosysEessiMelding melding = new MelosysEessiMelding();
        melding.setAktoerId(aktørID);
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
        melding.setSedType("A009");
        melding.setBucType("LA_BUC_04");
        return melding;
    }
}
