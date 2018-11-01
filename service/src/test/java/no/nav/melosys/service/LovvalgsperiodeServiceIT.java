package no.nav.melosys.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;

@RunWith(SpringRunner.class)
@DataJpaTest
@EnableAutoConfiguration(exclude = { WebMvcAutoConfiguration.class })
@EnableJpaRepositories(basePackageClasses = LovvalgsperiodeRepository.class)
@EntityScan(basePackageClasses = { Lovvalgsperiode.class })
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ComponentScan(useDefaultFilters = false, 
               basePackageClasses = { LovvalgsperiodeService.class,
                                      LovvalgsperiodeRepository.class },
               includeFilters = 
                   @Filter(type = FilterType.REGEX, 
                       pattern = { "no.nav.melosys.service.LovvalgsperiodeService",
                                   "no.nav.melosys.repository.LovvalgsperiodeRepository" }))
public class LovvalgsperiodeServiceIT {

    @SpringBootConfiguration
    static class Config {
    }

    private static final Instant ENDRET_DATO = Instant.ofEpochMilli(0);

    private static final long IKKE_EKSISTERENDE_BEH_ID = Long.MAX_VALUE;

    @Autowired
    private LovvalgsperiodeService instans;

    @Autowired
    private LovvalgsperiodeRepository repo;

    @Autowired
    private BehandlingsresultatRepository behandlingsresultatRepo;

    @Autowired
    private BehandlingRepository behandlingRepo;

    @Autowired
    private FagsakRepository fagsakRepo;

    private Lovvalgsperiode testInstans;

    @Before
    public void ryddOgOpprettTestdata() {
        repo.delete(repo.findByBehandlingsresultatId(IKKE_EKSISTERENDE_BEH_ID));
        testInstans = opprettTestdata();
    }

    private Lovvalgsperiode opprettTestdata() {
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultat();
        Lovvalgsperiode rad = lagLovvalgsperiode(behandlingsresultat);
        return repo.save(rad);
    }

    private Behandlingsresultat opprettBehandlingsresultat() {
        Fagsak fagsak = fagsakRepo.save(lagFagsak());
        Behandling behandling = behandlingRepo.save(lagBehandling(fagsak));
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.save(lagBehandlingsresultat(behandling));
        return behandlingsresultat;
    }

    private static Lovvalgsperiode lagLovvalgsperiode(Behandlingsresultat behandlingsresultat) {
        Lovvalgsperiode rad = new Lovvalgsperiode();
        rad.setBehandlingsresultat(behandlingsresultat);
        rad.setFom(LocalDate.now());
        rad.setTom(LocalDate.now());
        rad.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        rad.setBestemmelse(LovvalgBestemmelse_883_2004.ART11_1);
        rad.setLovvalgsland(Landkoder.SK);
        rad.setUnntakFraLovvalgsland(Landkoder.SI);
        rad.setUnntakFraBestemmelse(LovvalgBestemmelse_883_2004.ART11_3A);
        rad.setMedlemskapstype(Medlemskapstype.UNNTATT);
        rad.setBrukerID("Bruker Mye");
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

    @Test
    public void hentEnLovvalgsperiode() {
        Collection<Lovvalgsperiode> resultat = instans.hentLovvalgsperioder(testInstans.getBehandlingsresultat().getId());
        assertThat(resultat).hasSize(1);
    }

    @Test
    public void hentIngenLovvalgsperioder() {
        Collection<Lovvalgsperiode> resultat = instans.hentLovvalgsperioder(IKKE_EKSISTERENDE_BEH_ID);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void lagreEnLovvalgsperiodeGirKopiMedGenerertId() {
        Lovvalgsperiode periode = lagLovvalgsperiode(testInstans.getBehandlingsresultat());
        Collection<Lovvalgsperiode> lovvalgsperioder = Collections.singleton(periode);
        Collection<Lovvalgsperiode> resultat = instans.lagreLovvalgsperioder(testInstans.getBehandlingsresultat().getId(), lovvalgsperioder);
        assertThat(resultat).hasSize(1);
        assertThat(resultat.iterator().next().getId()).isNotEqualTo(0);
    }

    @Test
    public void lagreIngenLovvalgsperioderErNoop() {
        Collection<Lovvalgsperiode> lovvalgsperioder = Collections.emptyList();
        Collection<Lovvalgsperiode> resultat = instans.lagreLovvalgsperioder(testInstans.getBehandlingsresultat().getId(), lovvalgsperioder);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void lagreLovvalgsperiodePåIkkeEkisterendeBehandlingGirTomListe() {
        Lovvalgsperiode periode = lagLovvalgsperiode(testInstans.getBehandlingsresultat());
        Collection<Lovvalgsperiode> lovvalgsperioder = Collections.singleton(periode);
        Collection<Lovvalgsperiode> resultat = instans.lagreLovvalgsperioder(IKKE_EKSISTERENDE_BEH_ID, lovvalgsperioder);
        assertThat(resultat).isEmpty();
    }

}
