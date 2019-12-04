package no.nav.melosys.audit;

import javax.transaction.Transactional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = no.nav.melosys.PersistenceConfig.class)
@OverrideAutoConfiguration(enabled = false)
@EnableJpaAuditing
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = {WebMvcAutoConfiguration.class})
@ComponentScan
@EntityScan(basePackages = {"no.nav.melosys.domain"})
public class FagsakAuditingIT {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditorProvider auditorAware;

    @Test
    @Rollback(false)
    public void insertFagSak() throws Exception {

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("TEST-667");
        fagsak.setGsakSaksnummer(124L);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(Sakstyper.TRYGDEAVTALE);
        assertThat(entityManager.persistAndGetId(fagsak)).isNotNull();
    }
}