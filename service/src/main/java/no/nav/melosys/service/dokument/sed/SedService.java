package no.nav.melosys.service.dokument.sed;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1;

@Service
public class SedService {

    private static final Logger log = LoggerFactory.getLogger(SedService.class);

    private final SedDataByggerVelger sedDataByggerVelger;
    private final EessiConsumer eessiConsumer;
    private final boolean skalSendeSed;

    public SedService(SedDataByggerVelger sedDataByggerVelger, EessiConsumer eessiConsumer, @Value("${MelosysEessi.forsokSendSed:true}") String skalSendeSed) {
        this.sedDataByggerVelger = sedDataByggerVelger;
        this.eessiConsumer = eessiConsumer;
        this.skalSendeSed = Boolean.valueOf(skalSendeSed);
    }

    public void opprettOgSendSed(Behandling behandling, Behandlingsresultat behandlingsresultat) {

        if (skalSendeSed) {
            try {
                Lovvalgsperiode lovvalgsperiode = behandlingsresultat.getLovvalgsperioder().stream()
                    .findFirst().orElseThrow(() -> new TekniskException("Finner ingen lovvalgsperiode!")); //TODO: flere lovvalgsperioder

                LovvalgBestemmelse lovvalgBestemmelse = lovvalgsperiode.getBestemmelse();
                Fagsak fagsak = behandling.getFagsak();

                SedDataBygger sedDataBygger = sedDataByggerVelger.hent(lovvalgsperiode.getBestemmelse());
                SedDataDto sedData = sedDataBygger.lag(behandling);
                sedData.setGsakSaksnummer(fagsak.getGsakSaksnummer());

                log.info("Oppretter buc og sed med artikkel {} for fagsak {}", lovvalgBestemmelse.getKode(), fagsak.getSaksnummer());
                Map<String, String> rinaSakInfo = eessiConsumer.opprettOgSendSed(sedData);

                log.info("Buc opprettet med id {} for behandling {}", rinaSakInfo.get("rinaCaseId"), behandling.getId());
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

    public OpprettSedDto opprettBucOgSed(Behandling behandling, String bucType, String mottakerLand, String mottakerId) {
        if (skalSendeSed) {
            try {

                SedDataBygger sedDataBygger = sedDataByggerVelger.hent(FO_883_2004_ART12_1); // TODO: Må kunne hente uten lovvalgsbestemmelse
                SedDataDto sedDataDto = sedDataBygger.lag(behandling);

                sedDataDto.setMottakerLand(mottakerLand);
                sedDataDto.setMottakerId(mottakerId);
                sedDataDto.setGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());

                return eessiConsumer.opprettBucOgSed(sedDataDto, bucType);
            } catch (MelosysException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public List<SedinfoDto> hentTilknyttedeSeder(long gsakSaksnummer) {
        if (skalSendeSed) {
            try {
                return eessiConsumer.hentTilknyttedeSeder(gsakSaksnummer);
            } catch (MelosysException e) {
                log.error(e.getMessage());
            }
        }

        return Collections.emptyList();
    }
}
