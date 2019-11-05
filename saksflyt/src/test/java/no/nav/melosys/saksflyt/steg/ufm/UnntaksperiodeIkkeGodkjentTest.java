package no.nav.melosys.saksflyt.steg.ufm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeIkkeGodkjentTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private MedlFasade medlFasade;

    private UnntaksperiodeIkkeGodkjent unntaksperiodeIkkeGodkjent;

    private Behandling behandling = new Behandling();
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Before
    public void setup() {
        unntaksperiodeIkkeGodkjent = new UnntaksperiodeIkkeGodkjent(behandlingRepository, behandlingsresultatRepository, medlFasade);

        behandlingsresultat.getLovvalgsperioder().add(new Lovvalgsperiode());
        when(behandlingsresultatRepository.findById(any())).thenReturn(Optional.of(behandlingsresultat));
    }

    @Test
    public void utfør_medBegrunnelser_blirLagret() throws Exception {

        List<Ikke_godkjent_begrunnelser> ikkeGodkjentBegrunnelser = new ArrayList<>();
        ikkeGodkjentBegrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
        ikkeGodkjentBegrunnelser.add(Ikke_godkjent_begrunnelser.ANNET);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.getBehandling().setFagsak(new Fagsak());
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSER, ikkeGodkjentBegrunnelser);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST, "fritekst");

        unntaksperiodeIkkeGodkjent.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET);
        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        assertThat(behandlingsresultat.getUtfallRegistreringUnntak()).isEqualTo(Utfallregistreringunntak.IKKE_GODKJENT);
        assertThat(behandlingsresultat.getBegrunnelseFritekst()).isEqualTo("fritekst");
        assertThat(behandlingsresultat.getBehandlingsresultatBegrunnelser().stream().map(BehandlingsresultatBegrunnelse::getKode).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.name(), Ikke_godkjent_begrunnelser.ANNET.name());
    }

    @Test(expected = TekniskException.class)
    public void utfør_utenBegrunnelser_kasterException() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        unntaksperiodeIkkeGodkjent.utfør(prosessinstans);
    }
}