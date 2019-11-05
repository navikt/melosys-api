package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.iv.AvklarMyndighet;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarMyndighetTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    private AvklarMyndighet steg;

    private Prosessinstans p;

    @Before
    public void setUp() throws IkkeFunnetException {
        steg = new AvklarMyndighet(behandlingService, behandlingsresultatService, utenlandskMyndighetService);

        p = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");

        Behandling behandling = lagBehandling(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        p.setBehandling(behandling);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

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
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        steg.utfør(p);

        verify(utenlandskMyndighetService).avklarUtenlandskMyndighetSomAktørOgLagre(any(Behandling.class));
    }

    private static Behandlingsresultat lagBehandlingResultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }

    @Test
    public void utfør_iverksettVedtakSjekkSteg_forventAvklarArbeidsgiver() throws Exception {
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_AVKLAR_ARBEIDSGIVER);
    }

    @Test
    public void utfør_anmodningUnntakSjekkSteg_forventAouOppdaterMedl() throws Exception {
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);
        no.nav.melosys.saksflyt.steg.aou.ut.AvklarMyndighet steg =
            new no.nav.melosys.saksflyt.steg.aou.ut.AvklarMyndighet(behandlingService, behandlingsresultatService, utenlandskMyndighetService);
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AOU_OPPDATER_MEDL);
    }
}