package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaksopplysningerServiceTest {

    private SaksopplysningerService saksopplysningerService;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private AaregFasade aaregFasade;

    @Mock
    private ProsessinstansService prosessinstansService;

    @Mock
    private BehandlingRepository behandlingRepo;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Before
    public void setUp() {
        saksopplysningerService = new SaksopplysningerService(tpsFasade, aaregFasade, prosessinstansService, behandlingRepo, behandlingsresultatService);
    }

    @Test
    public void hentArbeidsforholdHistorikk() throws MelosysException {
        final Long arbeidsforholdsID = 12608035L;
        when(aaregFasade.hentArbeidsforholdHistorikk(eq(arbeidsforholdsID))).thenReturn(new Saksopplysning());
        saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        verify(aaregFasade).hentArbeidsforholdHistorikk(eq(arbeidsforholdsID));
    }

    @Test
    public void oppfriskSaksopplysning() throws IkkeFunnetException, TekniskException {

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Aktoer aktør = new Aktoer();
        aktør.setAktørId("123");
        aktør.setRolle(Aktoersroller.BRUKER);
        HashSet<Aktoer> aktører = new HashSet<>();
        aktører.add(aktør);
        fagsak.setAktører(aktører);
        behandling.setFagsak(fagsak);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysninger.add(saksopplysningPerson);

        SoeknadDokument soeknadDokument = new SoeknadDokument();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        soeknadDokument.arbeidUtland = new ArrayList<>();
        soeknadDokument.arbeidUtland.add(arbeidUtland);

        soeknadDokument.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));

        Saksopplysning saksopplysningSøknad = new Saksopplysning();
        saksopplysningSøknad.setType(SaksopplysningType.SØKNAD);
        saksopplysningSøknad.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysningSøknad);

        behandling.setSaksopplysninger(saksopplysninger);

        when(prosessinstansService.harAktivProsessinstans(anyLong())).thenReturn(false);
        when(behandlingRepo.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("12345");

        saksopplysningerService.oppfriskSaksopplysning(13L);

        assertThat(behandling.getSaksopplysninger().size()).isEqualTo(1);
        assertThat(behandling.getSaksopplysninger().stream().findFirst().get().getType()).isEqualTo(SaksopplysningType.SØKNAD);
        verify(behandlingsresultatService).tømBehandlingsresultat(anyLong());
    }
}