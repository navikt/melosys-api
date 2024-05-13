package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
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
    public void setUp() {
        avklarMyndighet = new AvklarMyndighet(behandlingService, behandlingsresultatService, utenlandskMyndighetService);

        prosessinstans = new Prosessinstans();
        Fagsak fagsak = FagsakTestFactory.lagFagsak();

        Behandling behandling = lagBehandling(fagsak);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK_EOS);
    }

    @Test
    void utfør_utenMyndighet_myndighetOpprettes() {

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        avklarMyndighet.utfør(prosessinstans);

        verify(utenlandskMyndighetService).avklarUtenlandskMyndighetSomAktørOgLagre(any(Behandling.class));
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        Soeknad søknadDokument = new Soeknad();
        søknadDokument.soeknadsland.getLandkoder().add("BE");
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.getAdresse().setLandkode("HR");
        søknadDokument.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);
        søknadDokument.bosted.getOppgittAdresse().setLandkode("IT");
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(søknadDokument);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);
        return behandling;
    }


    private static Behandlingsresultat lagBehandlingResultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }
}
