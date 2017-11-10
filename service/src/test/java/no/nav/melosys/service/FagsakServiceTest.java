package no.nav.melosys.service;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.Assert.assertNotNull;

public class FagsakServiceTest {

    @Mock
    private FagsakRepository fagsakRepo;

    @Before
    public void setUp() {
        fagsakRepo = Mockito.mock(FagsakRepository.class);
    }

    @Test
    public void lagFagsak() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        fagsak.setType(FagsakType.SØKNAD_A1);
        fagsak.setRegistrertDato(LocalDateTime.now());

        fagsakRepo.save(fagsak);

        assertNotNull(fagsak);
    }

}