package no.nav.melosys.service.dokument.sed;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SedService {

    private static final Logger log = LoggerFactory.getLogger(SedService.class);

    private final SedDataByggerVelger sedDataByggerVelger;
    private final FagsakRepository fagsakRepository;
    private final EessiConsumer eessiConsumer;

    public SedService(SedDataByggerVelger sedDataByggerVelger, FagsakRepository fagsakRepository, EessiConsumer eessiConsumer) {
        this.sedDataByggerVelger = sedDataByggerVelger;
        this.fagsakRepository = fagsakRepository;
        this.eessiConsumer = eessiConsumer;
    }

    // SED-er sendes ikke i Lev. 1
    public void opprettOgSendSed() throws MelosysException {
    }

    public void opprettOgSendSed(Behandling behandling, Behandlingsresultat behandlingsresultat) throws MelosysException {

        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.getLovvalgsperioder().stream()
            .findFirst().orElseThrow(() -> new TekniskException("Finner ingen lovvalgsperiode!")); //TODO: flere lovvalgsperioder

        LovvalgBestemmelse lovvalgBestemmelse = lovvalgsperiode.getBestemmelse();

        SedDataBygger sedDataBygger = sedDataByggerVelger.hent(lovvalgsperiode.getBestemmelse());
        SedDataDto sedData = sedDataBygger.lag(behandling);

        Fagsak fagsak = behandling.getFagsak();

        log.info("Oppretter buc og sed med artikkelt {} for fagsak {}", lovvalgBestemmelse.getKode(), fagsak.getSaksnummer());
        Map<String,String> rinaSakInfo = eessiConsumer.opprettOgSendSed(sedData);
        String rinaSaksnummer = rinaSakInfo.get("rinaCaseId");

        fagsak.setRinasaksnummer(rinaSaksnummer);
        fagsakRepository.save(fagsak);

    }
}
