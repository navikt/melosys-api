package no.nav.melosys.saksflyt.steg.ul;

import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    @Mock
    private MedlFasade medlFasade;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;

    private MedlPeriodeService felles;
    private OppdaterMedl oppdaterMedl;

    private Prosessinstans prosessinstans;

    @Before
    public void settOpp() throws IkkeFunnetException {
        felles = new MedlPeriodeService(
            mock(TpsFasade.class), medlFasade, behandlingsresultatService,
            lovvalgsperiodeRepository, mock(AnmodningsperiodeRepository.class));
        oppdaterMedl = new OppdaterMedl(medlFasade, felles);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-0");

        Behandling behandling = new Behandling();
        behandling.setId(0L);
        behandling.setFagsak(fagsak);

        Aktoer aktør = new Aktoer();
        aktør.setAktørId("00000000000");
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(aktør));

        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(new Lovvalgsperiode()));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utfør() throws MelosysException {
        oppdaterMedl.utfør(prosessinstans);

        verify(medlFasade).opprettPeriodeForeløpig(any(), any(), any());
        verify(lovvalgsperiodeRepository).save(any(Lovvalgsperiode.class));

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.UL_SEND_ORIENTERINGSBREV);
    }
}