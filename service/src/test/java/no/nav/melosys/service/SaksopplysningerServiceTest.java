package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
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
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private BehandlingRepository behandlingRepo;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private MedlFasade medlFasade;

    private Integer medlemskaphistorikkAntallÅr = 5;

    private SaksopplysningerService saksopplysningerService;

    @Before
    public void setUp() {
        saksopplysningerService = new SaksopplysningerService(tpsFasade, prosessinstansService,
            behandlingRepo, behandlingsresultatService, saksopplysningRepository, medlFasade, medlemskaphistorikkAntallÅr);
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
        saksopplysningPerson.setType(SaksopplysningType.PERSOPL);
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

    @Test
    public void hentSaksopplysningMedl() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        final long behandlingID = 11;
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setFagsak(new Fagsak());
        when(behandlingRepo.findById(eq(behandlingID))).thenReturn(Optional.of(behandling));

        final String aktørID = "2222";
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(aktørID);
        behandling.getFagsak().getAktører().add(aktoer);

        final String brukerID = "432534";
        final Saksopplysning medlSaksopplysning = new Saksopplysning();
        when(tpsFasade.hentIdentForAktørId(eq(aktørID))).thenReturn(brukerID);
        when(medlFasade.hentPeriodeListe(eq(brukerID), any(LocalDate.class), any(LocalDate.class))).thenReturn(medlSaksopplysning);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));

        saksopplysningerService.hentSaksopplysningMedl(behandlingID, lovvalgsperiode);

        verify(behandlingRepo).save(eq(behandling));
        verify(tpsFasade).hentIdentForAktørId(eq(aktørID));
        verify(medlFasade).hentPeriodeListe(eq(brukerID), eq(lovvalgsperiode.getFom().minusYears(5)), eq(lovvalgsperiode.getTom()));
        verify(saksopplysningRepository).save(eq(medlSaksopplysning));
    }
}