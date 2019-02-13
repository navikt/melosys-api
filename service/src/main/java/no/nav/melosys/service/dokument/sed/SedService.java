package no.nav.melosys.service.dokument.sed;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void opprettOgSendSed(Behandling behandling, Behandlingsresultat behandlingsresultat) throws MelosysException {

        try {
            Lovvalgsperiode lovvalgsperiode = behandlingsresultat.getLovvalgsperioder().stream()
                .findFirst().orElseThrow(() -> new TekniskException("Finner ingen lovvalgsperiode!")); //TODO: flere lovvalgsperioder

            LovvalgBestemmelse lovvalgBestemmelse = lovvalgsperiode.getBestemmelse();

            SedDataBygger sedDataBygger = sedDataByggerVelger.hent(lovvalgsperiode.getBestemmelse());
            SedDataDto sedData = sedDataBygger.lag(behandling);

            Fagsak fagsak = fagsakRepository.findBySaksnummer(behandling.getFagsak().getSaksnummer());

            log.info("Oppretter buc og sed med artikkel {} for fagsak {}", lovvalgBestemmelse.getKode(), fagsak.getSaksnummer());
            Map<String, String> rinaSakInfo = eessiConsumer.opprettOgSendSed(sedData);
            String rinaSaksnummer = rinaSakInfo.get("rinaCaseId");

            fagsak.setRinasaksnummer(rinaSaksnummer);
            fagsakRepository.save(fagsak);
            log.info("Buc opprettet med id {} for behandling {}", rinaSaksnummer, behandling.getId());
        } catch (Exception e) {
            log.error(
                "Feil ved opprettelse av SED: \n" +
                "Behandling {}\n" +
                "Behandlingsresultat {}\n" +
                "Fagsak {}\n",
                behandling.getId(), behandlingsresultat.getId(), behandling.getFagsak().getSaksnummer(), e);
        }

    }
}
