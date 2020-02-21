package no.nav.melosys.integrasjonstest.felles.verifisering;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Component
@Scope("prototype")
public class ProsessinstansTestService {

    @Autowired
    ProsessinstansRepository prosessinstansRepository;

    private static final List<ProsessSteg> stegFerdigFeilet = Arrays.asList(ProsessSteg.FERDIG, ProsessSteg.FEILET_MASKINELT);

    public void ventPå(long behandlingId, ProsessSteg... prosessSteg) {
        Collection<ProsessSteg> forventedeProsessSteg = ListUtils.union(stegFerdigFeilet, Arrays.asList(prosessSteg));
        await()
            .atMost(2, TimeUnit.MINUTES)
            .pollInterval(3, TimeUnit.SECONDS)
            .until(() -> sjekkSteg(behandlingId, forventedeProsessSteg));
    }

    public boolean sjekkSteg(long behandlingId, Collection<ProsessSteg> forventedeProsessSteg) {
        List<ProsessSteg> lagredeProsessSteg = hentProsessStegForBehandling(behandlingId);
        return !Collections.disjoint(lagredeProsessSteg, forventedeProsessSteg);
    }

    public void sjekkProsessteg(long behandlingId, ProsessSteg forventetSteg) {
        List<ProsessSteg> lagredeProsessSteg = hentProsessStegForBehandling(behandlingId);
        assertThat(lagredeProsessSteg).containsExactly(forventetSteg);
    }

    public List<ProsessSteg> hentProsessStegForBehandling(long behandlingsid) {
        return prosessinstansRepository.findAll().stream()
            .filter(pi -> pi.getBehandling().getId() == behandlingsid)
            .map(Prosessinstans::getSteg)
            .collect(Collectors.toList());
    }

    public void nullstill() {
        prosessinstansRepository.deleteAll();
    }
}