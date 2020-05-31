package no.nav.melosys.saksflyt.steg.afl;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class VurderInngangsvilkaarTest {
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private VurderInngangsvilkaar vurderInngangsvilkaar;

    private Prosessinstans prosessinstans = new Prosessinstans();
    private Behandling behandling = new Behandling();
    private SedGrunnlag sedGrunnlag = new SedGrunnlag();

    @Before
    public void setup() throws FunksjonellException, TekniskException {
        vurderInngangsvilkaar = new VurderInngangsvilkaar(inngangsvilkaarService, fagsakService, behandlingsgrunnlagService);

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(sedGrunnlag);
        sedGrunnlag.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1));

        behandling.setId(11L);
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);

        prosessinstans.setBehandling(behandling);

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(eq(behandling.getId()))).thenReturn(behandlingsgrunnlag);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(eq(behandling.getId()), any(), any())).thenReturn(true);
    }

    @Test
    public void utfør_arbeidsstedISverige_sverigeVurderesSomSøknadsland() throws FunksjonellException, TekniskException {
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landkode = "SE";
        sedGrunnlag.arbeidUtland = List.of(arbeidUtland);

        vurderInngangsvilkaar.utfør(prosessinstans);

        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(anyLong(), eq(List.of("SE")), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AFL_REGISTERKONTROLL);
    }

    @Test
    public void utfør_ingenArbeidssteder_norgeVurderesSomSøknadsland() throws FunksjonellException, TekniskException {
        vurderInngangsvilkaar.utfør(prosessinstans);

        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(anyLong(), eq(List.of("NO")), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AFL_REGISTERKONTROLL);
    }

    @Test
    public void utførikkeUtpekingAvNorge_kasterException() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> vurderInngangsvilkaar.utfør(prosessinstans))
            .withMessageContaining("Steget vurderer inngangsvilkår når Norge er utpekt, ikke for ");
    }
}