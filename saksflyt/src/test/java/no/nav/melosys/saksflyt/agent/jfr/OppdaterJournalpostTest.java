package no.nav.melosys.saksflyt.agent.jfr;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.joark.JournalfoeringMangel;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.FagsakRepository;
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
public class OppdaterJournalpostTest {

    @Mock
    private JoarkFasade joarkFasade;

    private OppdaterJournalpost agent;

    @Before
    public void setUp() throws Exception {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        agent = new OppdaterJournalpost(joarkFasade, fagsakRepository);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        p.setData(ProsessDataKey.GSAK_SAK_ID, "Grønnsak id");
        agent.utførSteg(p);

        verify(joarkFasade, times(1)).utledJournalfoeringsbehov(any());
        verify(joarkFasade, times(1)).oppdaterJounalpost(any(), any(), any(), any(), any(), any(), any(), eq(false));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST);
    }

    @Test
    public void utfoerSteg_oppdaterDokumentKategori() throws SikkerhetsbegrensningException {
        List<JournalfoeringMangel> mangler = new ArrayList<>();
        mangler.add(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        when(joarkFasade.utledJournalfoeringsbehov(any())).thenReturn(mangler);
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        p.setData(ProsessDataKey.GSAK_SAK_ID, "Grønnsak id");

        agent.utførSteg(p);

        verify(joarkFasade, times(1)).utledJournalfoeringsbehov(any());
        verify(joarkFasade, times(1)).oppdaterJounalpost(any(), any(), any(), any(), any(), any(), any(), eq(true));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST);
    }
}