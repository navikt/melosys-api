package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvklarMyndighetTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    private AvklarMyndighet avklarMyndighet;

    private Prosessinstans prosessinstans;

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        avklarMyndighet = new AvklarMyndighet(behandlingService, behandlingsresultatService, utenlandskMyndighetService);

        prosessinstans = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");

        Behandling behandling = lagBehandling(fagsak);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
    }

    @Test
    void utfør_utenMyndighet_myndighetOpprettes() throws FunksjonellException, TekniskException {

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        avklarMyndighet.utfør(prosessinstans);

        verify(utenlandskMyndighetService).avklarUtenlandskMyndighetSomAktørOgLagre(any(Behandling.class));
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.SOEKNAD);
        Soeknad søknadDokument = new Soeknad();
        søknadDokument.soeknadsland.landkoder.add("BE");
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse.landkode = "HR";
        søknadDokument.arbeidPaaLand.fysiskeArbeidsstederUtland.add(fysiskArbeidssted);
        søknadDokument.bosted.oppgittAdresse.landkode = "IT";
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknadDokument);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
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
}