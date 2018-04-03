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

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;

@RunWith(SpringRunner.class)
@DataJpaTest
@Ignore
@SuppressWarnings("all")
public class FagsakRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FagsakRepository fagsakRepository;

    /* FIXME
    
    @Test
    public void findByFnr() throws Exception {

        Bruker bruker = new Bruker();
        bruker.setFnr("FJERNET4");
        bruker.setKjønn(Kjoenn.KVINNE);
        entityManager.persist(bruker);

        Fagsak fagsak = new Fagsak();
        fagsak.setBruker(bruker);
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        entityManager.persist(fagsak);

        List<Fagsak> byFnr = fagsakRepository.findByFnr("FJERNET4");

        assertThat(byFnr.size()).isEqualTo(1);
        Fagsak funnet = byFnr.get(0);
        assertThat(funnet.getGsakSaksnummer()).isEqualTo(123);

    }

    @Test
    public void findByFnrIkkeFunnet() throws Exception {

        Bruker bruker = new Bruker();
        bruker.setFnr("FJERNET4");
        bruker.setKjønn(Kjoenn.KVINNE);
        entityManager.persist(bruker);

        Fagsak fagsak = new Fagsak();
        fagsak.setBruker(bruker);
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        entityManager.persist(fagsak);

        List<Fagsak> byFnr = fagsakRepository.findByFnr("12345678901");

        assertThat(byFnr.size()).isEqualTo(0);
        
    }

    // */
}