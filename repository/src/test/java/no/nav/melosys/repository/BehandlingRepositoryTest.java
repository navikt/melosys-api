package no.nav.melosys.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;

@RunWith(SpringRunner.class)
@DataJpaTest
@Ignore
@SuppressWarnings("all")
public class BehandlingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    BehandlingRepository behandlingRepository;

    /* FIXME
    @Test
    public void testFindBySaksnummerIkkeFunnet() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        entityManager.persist(fagsak);

        Behandling behandling = new Behandling();
        behandling.setBehandlingsId(777L);
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandling.setType(BehandlingType.FØRSTEGANGSSØKNAD);
        behandling.setFagsak(fagsak);

        entityManager.persist(behandling);

        List<Behandling> bySaksnummer = behandlingRepository.findBySaksnummer(1L);
        assertThat(bySaksnummer.size()).isEqualTo(0);
    }

    @Test
    public void testFindBySaksnummer() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        entityManager.persist(fagsak);

        Behandling behandling = new Behandling();
        behandling.setBehandlingsId(777L);
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandling.setType(BehandlingType.FØRSTEGANGSSØKNAD);
        behandling.setFagsak(fagsak);

        entityManager.persist(behandling);


        List<Behandling> bySaksnummer = behandlingRepository.findBySaksnummer(123L);
        assertThat(bySaksnummer.size()).isEqualTo(1);
        assertThat(bySaksnummer.get(0)).isInstanceOf(Behandling.class);
        Behandling funnet = (Behandling) bySaksnummer.get(0);
        assertThat(funnet.getBehandlingsId()).isEqualTo(777L);
        assertThat(funnet.getFagsak()).isNotNull();
        assertThat(funnet.getFagsak().getGsakSaksnummer()).isEqualTo(123L);
    }
    //*/

}