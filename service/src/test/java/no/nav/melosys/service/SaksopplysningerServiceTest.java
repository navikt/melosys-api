package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaksopplysningerServiceTest {
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private BehandlingRepository behandlingRepo;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private SaksopplysningerService saksopplysningerService;

    @Before
    public void setUp() {
        saksopplysningerService = new SaksopplysningerService(tpsFasade, prosessinstansService,
            behandlingRepo, behandlingsresultatService, registeropplysningerService, saksopplysningRepository);
    }

    @Test
    public void oppfriskSaksopplysning() throws MelosysException {

        final String aktørID = "123";
        final String brukerID = "322211";

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(aktørID);
        aktør.setRolle(Aktoersroller.BRUKER);
        HashSet<Aktoer> aktører = new HashSet<>();
        aktører.add(aktør);
        fagsak.setAktører(aktører);
        behandling.setFagsak(fagsak);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSOPL);
        saksopplysninger.add(saksopplysningPerson);

        SoeknadDokument soeknadDokument = new SoeknadDokument();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        soeknadDokument.arbeidUtland = new ArrayList<>();
        soeknadDokument.arbeidUtland.add(arbeidUtland);

        soeknadDokument.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknadDokument);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        behandling.setSaksopplysninger(saksopplysninger);

        when(prosessinstansService.harAktivProsessinstans(anyLong())).thenReturn(false);
        when(behandlingRepo.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(brukerID);

        saksopplysningerService.oppfriskSaksopplysning(13L);

        verify(behandlingsresultatService).tømBehandlingsresultat(anyLong());
        verify(registeropplysningerService).hentOgLagreOpplysninger(any(RegisteropplysningerRequest.class));
    }
}