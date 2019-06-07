package no.nav.melosys.service.dokument.sed;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.Sedinformasjon;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SedService {

    private static final Logger log = LoggerFactory.getLogger(SedService.class);

    private final SedDataBygger sedDataBygger;
    private final EessiConsumer eessiConsumer;
    private final boolean skalSendeSed;

    public SedService(SedDataBygger sedDataBygger, EessiConsumer eessiConsumer, @Value("${MelosysEessi.forsokSendSed:true}") String skalSendeSed) {
        this.sedDataBygger = sedDataBygger;
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

    public List<Institusjon> hentMottakerinstitusjoner(String bucType) throws MelosysException {
        return eessiConsumer.hentMottakerinstitusjoner(bucType).stream()
            .map(institusjonDto -> new Institusjon(institusjonDto.getId(), institusjonDto.getNavn(), institusjonDto.getLandkode()))
            .collect(Collectors.toList());
    }

    public String opprettBucOgSed(Behandling behandling, String bucType, String mottakerLand, String mottakerId) throws MelosysException {
        if (skalSendeSed) {
            SedDataDto sedDataDto = sedDataBygger.lag(behandling);
            sedDataDto.setMottakerLand(mottakerLand);
            sedDataDto.setMottakerId(mottakerId);
            sedDataDto.setGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());

            log.info("Oppretter buc og sed for behandling {} med bucType {}", behandling.getId(), bucType);
            return eessiConsumer.opprettBucOgSed(sedDataDto, bucType).getRinaUrl();
        }

        throw new IllegalStateException("Ikke mulig å sende sed");
    }

    public List<Sedinformasjon> hentTilknyttedeSeder(long gsakSaksnummer, String status) throws MelosysException {
        return eessiConsumer.hentTilknyttedeSeder(gsakSaksnummer, status).stream()
            .map(SedService::tilSedinformasjon).collect(Collectors.toList());
    }

    private static Sedinformasjon tilSedinformasjon(SedinfoDto sedinfoDto) {
        return new Sedinformasjon(
            sedinfoDto.getBucId(),
            sedinfoDto.getSedId(),
            sedinfoDto.getOpprettetDato(),
            sedinfoDto.getSistOppdatert(),
            sedinfoDto.getSedType(),
            sedinfoDto.getStatus(),
            sedinfoDto.getRinaUrl()
        );
    }
}
