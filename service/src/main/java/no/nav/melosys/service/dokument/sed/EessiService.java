package no.nav.melosys.service.dokument.sed;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EessiService {

    private static final Logger log = LoggerFactory.getLogger(EessiService.class);

    private final SedDataBygger sedDataBygger;
    private final EessiConsumer eessiConsumer;
    private final boolean skalSendeSed;

    private static final List<SedType> AUTOMATISK_BEHANDLING_SED_TYPER = Arrays.asList(
        SedType.A001, SedType.A003, SedType.A009, SedType.A010
    );


    public EessiService(SedDataBygger sedDataBygger, EessiConsumer eessiConsumer, @Value("${MelosysEessi.forsokSendSed:true}") String skalSendeSed) {
        this.sedDataBygger = sedDataBygger;
        this.eessiConsumer = eessiConsumer;
        this.skalSendeSed = Boolean.valueOf(skalSendeSed);
    }

    public void opprettOgSendSed(Behandling behandling, Behandlingsresultat behandlingsresultat) {

        if (skalSendeSed) {
            try {
                Fagsak fagsak = behandling.getFagsak();

                SedDataDto sedData = sedDataBygger.lag(behandling, behandlingsresultat);
                sedData.setGsakSaksnummer(fagsak.getGsakSaksnummer());

                log.info("Oppretter buc og sed for fagsak {}", fagsak.getSaksnummer());
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
        return eessiConsumer.hentMottakerinstitusjoner(bucType);
    }

    public String opprettBucOgSed(Behandling behandling, String bucType, String mottakerLand, String mottakerId) throws MelosysException {
        if (skalSendeSed) {
            SedDataDto sedDataDto = sedDataBygger.lagUtkast(behandling);
            sedDataDto.setMottakerLand(mottakerLand);
            sedDataDto.setMottakerId(mottakerId);
            sedDataDto.setGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());

            log.info("Oppretter buc og sed for behandling {} med bucType {}", behandling.getId(), bucType);
            return eessiConsumer.opprettBucOgSed(sedDataDto, bucType);
        }

        throw new IllegalStateException("Ikke mulig å sende sed");
    }

    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, String status) throws MelosysException {
        return eessiConsumer.hentTilknyttedeBucer(gsakSaksnummer, status);
    }

    public boolean støtterAutomatiskBehandling(String journalpostID, String sedType) throws MelosysException {
        if (sedType == null || Arrays.stream(SedType.values()).map(SedType::name).noneMatch(s -> s.equals(sedType))) {
            return false;
        }
        SedType sedTypeEnum = SedType.valueOf(sedType);

        if (sedTypeEnum == SedType.A003) {
            return !norgeErUtpekt(journalpostID);
        }

        return AUTOMATISK_BEHANDLING_SED_TYPER.contains(sedTypeEnum);
    }

    private boolean norgeErUtpekt(String journalpostID) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = hentSedTilknyttetJournalpost(journalpostID);
        return Landkoder.NO.name().equals(melosysEessiMelding.getLovvalgsland());
    }

    public MelosysEessiMelding hentSedTilknyttetJournalpost(String journalpostID) throws MelosysException {
        return eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(journalpostID);
    }

    public Optional<Long> hentSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException {
        return eessiConsumer.hentSakForRinasaksnummer(rinaSaksnummer).stream()
            .findFirst().map(SaksrelasjonDto::getGsakSaksnummer);
    }

    public void lagreSaksrelasjon(Long gsakSaksnummer, String rinaSaksnummer, String bucType) throws MelosysException {
        eessiConsumer.lagreSaksrelasjon(new SaksrelasjonDto(gsakSaksnummer, rinaSaksnummer, bucType));
    }
}
