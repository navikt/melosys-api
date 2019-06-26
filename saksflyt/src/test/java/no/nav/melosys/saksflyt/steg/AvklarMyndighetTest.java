package no.nav.melosys.saksflyt.steg;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.steg.iv.AvklarMyndighet;
import no.nav.melosys.service.aktoer.AvklarMyndighetService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarMyndighetTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private AvklarMyndighetService avklarMyndighetService;

    private AvklarMyndighet steg;

    private Prosessinstans p;

    @Before
    public void setUp() {
        steg = new AvklarMyndighet(behandlingRepository, behandlingsresultatRepository, avklarMyndighetService);

        p = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");

        Behandling behandling = lagBehandling(fagsak);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        p.setBehandling(behandling);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.SOEKNAD);
        SoeknadDokument søknadDokument = new SoeknadDokument();
        søknadDokument.soeknadsland.landkoder.add("BE");
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landkode = "HR";
        søknadDokument.arbeidUtland.add(arbeidUtland);
        søknadDokument.bosted.oppgittAdresse.landkode = "IT";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setDokument(søknadDokument);
        behandling.getSaksopplysninger().add(saksopplysning);
        return behandling;
    }

    @Test
    public void utfør_utenMyndighet_myndighetOpprettes() throws FunksjonellException, TekniskException {

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));

        steg.utfør(p);

        verify(avklarMyndighetService).avklarUtenlandskMyndighetOgLagre(any(Behandling.class));
    }

    private static Behandlingsresultat lagBehandlingResultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }

    @Test
    public void utfør_medMyndighet_myndighetOpprettesIkke() throws FunksjonellException, TekniskException {
        Fagsak fagsakMedMyndighet = new Fagsak();
        fagsakMedMyndighet.setSaksnummer("med myndighet");
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        fagsakMedMyndighet.getAktører().add(myndighet);
        p.getBehandling().setFagsak(fagsakMedMyndighet);

        steg.utfør(p);

        verify(avklarMyndighetService, never()).avklarUtenlandskMyndighetOgLagre(any(Behandling.class));
    }

    @Test
    public void utfør_iverksettVedtakSjekkSteg_forventAvklarArbeidsgiver() throws Exception {
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_AVKLAR_ARBEIDSGIVER);
    }

    @Test
    public void utfør_anmodningUnntakSjekkSteg_forventAouOppdaterMedl() throws Exception {
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
        no.nav.melosys.saksflyt.steg.aou.AvklarMyndighet steg =
            new no.nav.melosys.saksflyt.steg.aou.AvklarMyndighet(behandlingRepository, behandlingsresultatRepository, avklarMyndighetService);
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AOU_OPPDATER_MEDL);
    }
}