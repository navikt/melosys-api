package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Periode;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeMottakInitialisererTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private UnntaksperiodeMottakInitialiserer unntaksperiodeMottakInitialiserer;

    @Before
    public void setup() {
        unntaksperiodeMottakInitialiserer = new UnntaksperiodeMottakInitialiserer(fagsakService, lovvalgsperiodeService);
    }

    @Test
    public void finnSakOgBestemRuting_nySak_verifiserResultatNySak() throws FunksjonellException {
        Prosessinstans prosessinstans = hentProsessinstans(LocalDate.now(), LocalDate.now().plusYears(1));

        RutingResultat resultat = unntaksperiodeMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);
        assertThat(resultat).isEqualTo(RutingResultat.NY_SAK);
    }

    @Test
    public void finnSakOgBestemRuting_oppdatertSedPåEksisterendeSakIkkeEndretPeriode_skalIkkeBehandles() throws Exception {

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusYears(1);
        Prosessinstans prosessinstans = hentProsessinstans(fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(lovvalgsperiode));
        RutingResultat resultat = unntaksperiodeMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);

        assertThat(resultat).isEqualTo(RutingResultat.INGEN_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_oppdatertSedPåEksisterendeSakErEndretPeriode_skalBehandles() throws Exception {
        LocalDate fom = LocalDate.now();
        LocalDate tom = null;
        Prosessinstans prosessinstans = hentProsessinstans(fom, tom);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom.plusMonths(1));
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));

        when(fagsakService.finnFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(lovvalgsperiode));

        RutingResultat resultat = unntaksperiodeMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, 1L);

        assertThat(resultat).isEqualTo(RutingResultat.NY_BEHANDLING);
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
        melding.setAktoerId("123");
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