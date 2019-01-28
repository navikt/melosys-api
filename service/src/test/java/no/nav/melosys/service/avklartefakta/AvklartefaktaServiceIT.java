package no.nav.melosys.service.avklartefakta;

import java.time.Instant;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@EnableAutoConfiguration(exclude = {WebMvcAutoConfiguration.class})
@EnableJpaRepositories(basePackageClasses = AvklarteFaktaRepository.class)
@EntityScan(basePackageClasses = {Avklartefakta.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ComponentScan(useDefaultFilters = false, basePackageClasses = {AvklartefaktaService.class,
    AvklarteFaktaRepository.class,},
    includeFilters = @Filter(type = FilterType.REGEX,
        pattern = {"no.nav.melosys.service.avklartefakta.AvklartefaktaService",
            "no.nav.melosys.repository.AvklarteFaktaRepository",
            "no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer"}))
public class AvklartefaktaServiceIT {

    @SpringBootConfiguration
    static class Config {
    }

    private static final long IKKE_EKSISTERENDE_BEH_ID = Long.MAX_VALUE;
    private static final Instant ENDRET_DATO = Instant.now();
    @Autowired
    private AvklartefaktaService instans;
    @Autowired
    private AvklarteFaktaRepository repo;
    @Autowired
    private BehandlingsresultatRepository behandlingsresultatRepo;
    @Autowired
    private BehandlingRepository behandlingRepo;
    @Autowired
    private FagsakRepository fagsakRepo;

    private Avklartefakta testInstans;

    private static Avklartefakta lagAvklarteFakta(Behandlingsresultat behandlingsresultat) {
        Avklartefakta rad = new Avklartefakta();
        rad.setBehandlingsresultat(behandlingsresultat);
        rad.setFakta("test test");
        rad.setReferanse("referanse");
        rad.setType(AvklartefaktaType.BOSTEDSLAND);
        return rad;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Behandling behandling) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setEndretAv("endret av");
        behandlingsresultat.setEndretDato(ENDRET_DATO);
        behandlingsresultat.setRegistrertAv("Avregistrerer");
        behandlingsresultat.setRegistrertDato(ENDRET_DATO);
        behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.AUTOMATISERT);
        behandlingsresultat.setType(BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
        return behandlingsresultat;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setEndretDato(ENDRET_DATO);
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(ENDRET_DATO);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstype.KLAGE);
        return behandling;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        fagsak.setEndretDato(ENDRET_DATO);
        fagsak.setRegistrertDato(ENDRET_DATO);
        fagsak.setStatus(Fagsaksstatus.AVSLUTTET);
        return fagsak;
    }

    @Before
    public void ryddOgOpprettTestdata() {
        repo.deleteAll(repo.findByBehandlingsresultatId(IKKE_EKSISTERENDE_BEH_ID));
        testInstans = opprettTestdata();
    }

    private Avklartefakta opprettTestdata() {
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultat();
        Avklartefakta rad = lagAvklarteFakta(behandlingsresultat);
        return repo.save(rad);
    }

    private Behandlingsresultat opprettBehandlingsresultat() {
        Fagsak fagsak = fagsakRepo.save(lagFagsak());
        Behandling behandling = behandlingRepo.save(lagBehandling(fagsak));
        return behandlingsresultatRepo.save(lagBehandlingsresultat(behandling));
    }

    @Test
    public void lagreAvklarteFakta() throws IkkeFunnetException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("test fakta");
        avklartefakta.setReferanse("gammel referanse");
        HashSet<AvklartefaktaDto> avklartefaktaDtoer = new HashSet<>();
        avklartefaktaDtoer.add(new AvklartefaktaDto(avklartefakta));
        instans.lagreAvklarteFakta(testInstans.getBehandlingsresultat().getId(), avklartefaktaDtoer);
        assertThat(repo.findByBehandlingsresultatId(testInstans.getBehandlingsresultat().getId()).size()).isEqualTo(1);

        avklartefaktaDtoer.clear();

        avklartefakta.setFakta("ny fakta");
        avklartefakta.setReferanse("ny referanse");
        avklartefaktaDtoer.add(new AvklartefaktaDto(avklartefakta));
        instans.lagreAvklarteFakta(testInstans.getBehandlingsresultat().getId(), avklartefaktaDtoer);
        Avklartefakta avklartefaktaFraRepo = repo.findByBehandlingsresultatId(testInstans.getBehandlingsresultat().getId()).iterator().next();
        assertThat(avklartefaktaFraRepo.getReferanse()).isEqualTo("ny referanse");
    }
}
