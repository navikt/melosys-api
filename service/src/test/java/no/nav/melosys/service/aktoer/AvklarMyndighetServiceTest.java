package no.nav.melosys.service.aktoer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklarMyndighetServiceTest {

    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    @Mock
    private FagsakService fagsakService;

    private AvklarMyndighetService avklarMyndighetService;

    private Behandling behandling;

    private String forventetInstitusjonId;

    @Captor
    ArgumentCaptor<List<String>> stringListArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        avklarMyndighetService = new AvklarMyndighetService(utenlandskMyndighetRepository, landvelgerService, fagsakService);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");

        behandling = new Behandling();
        behandling.setFagsak(fagsak);

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.institusjonskode = "IT123";
        utenlandskMyndighet.land = "IT";

        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.IT))).thenReturn(Optional.of(utenlandskMyndighet));
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(any(Behandling.class))).thenReturn(Collections.singletonList(Landkoder.IT));

        forventetInstitusjonId = Landkoder.IT + ":" + utenlandskMyndighet.institusjonskode;
    }

    @Test
    public void lagUtenlandskMyndighetFraBehandling_forventAktoerMedGyldigInstitusjonsId() throws Exception {
        Collection<Aktoer> aktoerer = avklarMyndighetService.lagUtenlandskMyndighetFraBehandling(behandling);
        assertThat(aktoerer).isNotEmpty();
        assertThat(aktoerer.iterator().next().getInstitusjonId()).isEqualTo(forventetInstitusjonId);
    }

    @Test
    public void avklarMyndighetOgLagre_forventkorrektInstitusjonsId() throws Exception {
        avklarMyndighetService.avklarUtenlandskMyndighetOgLagre(behandling);

        verify(fagsakService).leggTilFjernAktørerForMyndighet(eq(behandling.getFagsak().getSaksnummer()), stringListArgumentCaptor.capture());
        assertThat(stringListArgumentCaptor.getValue()).containsExactly(forventetInstitusjonId);
    }
}