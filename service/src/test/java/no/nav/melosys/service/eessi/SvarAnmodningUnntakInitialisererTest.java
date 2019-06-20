package no.nav.melosys.service.eessi;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SvarAnmodningUnntakInitialisererTest {

    private SvarAnmodningUnntakInitialiserer svarAnmodningUnntakInitialiserer;

    @Mock
    private FagsakService fagsakService;

    @Before
    public void setUp() {
        svarAnmodningUnntakInitialiserer = new SvarAnmodningUnntakInitialiserer(fagsakService);
    }

    @Test
    public void initialiserProsessinstans_a011IngenYtterligereInformasjon_registreresAutomatisk() throws Exception {

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        Prosessinstans prosessinstans = hentProsessinstans(SedType.A011, false);
        svarAnmodningUnntakInitialiserer.initialiserProsessinstans(prosessinstans);

        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
    }

    @Test(expected = TekniskException.class)
    public void initialiserProsessinstans_a011MedYtterligereInformasjon_registreresAutomatisk() throws Exception {

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        Prosessinstans prosessinstans = hentProsessinstans(SedType.A011, true);
        svarAnmodningUnntakInitialiserer.initialiserProsessinstans(prosessinstans);
    }

    @Test(expected = TekniskException.class)
    public void initialiserProsessinstans_a002IngenYtterligereInformasjon_registreresIkkeAutomatisk() throws Exception {

        when(fagsakService.hentFagsakFraGsakSaksnummer(anyLong())).thenReturn(Optional.of(hentFagsak()));
        Prosessinstans prosessinstans = hentProsessinstans(SedType.A002, false);
        svarAnmodningUnntakInitialiserer.initialiserProsessinstans(prosessinstans);
    }

    private Fagsak hentFagsak() {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }

    private Prosessinstans hentProsessinstans(SedType sedType, boolean medYtterligereInfo) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, hentMelosysEessiMelding(sedType, medYtterligereInfo));
        return prosessinstans;
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType, boolean medYtterligereInfo) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setGsakSaksnummer(123L);
        melosysEessiMelding.setSedType(sedType.name());
        if (medYtterligereInfo) {
            melosysEessiMelding.setYtterligereInformasjon("hei");
        }

        return melosysEessiMelding;
    }
}