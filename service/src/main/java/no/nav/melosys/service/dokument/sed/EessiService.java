package no.nav.melosys.service.dokument.sed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SvarAnmodningUnntakDto;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlagFactory;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EessiService {
    private static final Logger log = LoggerFactory.getLogger(EessiService.class);

    private final SedDataBygger sedDataBygger;
    private final DokumentdataGrunnlagFactory dokumentdataGrunnlagFactory;
    private final EessiConsumer eessiConsumer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final boolean skalSendeSed;

    private static final List<SedType> AUTOMATISK_BEHANDLING_SED_TYPER = Arrays.asList(
        SedType.A001, SedType.A003, SedType.A009, SedType.A010
    );

    public EessiService(@Value("${MelosysEessi.forsokSendSed:true}") String skalSendeSed, SedDataBygger sedDataBygger,
                        DokumentdataGrunnlagFactory dokumentdataGrunnlagFactory, EessiConsumer eessiConsumer,
                        BehandlingService behandlingService,
                        BehandlingsresultatService behandlingsresultatService) {
        this.skalSendeSed = Boolean.parseBoolean(skalSendeSed);
        this.sedDataBygger = sedDataBygger;
        this.dokumentdataGrunnlagFactory = dokumentdataGrunnlagFactory;
        this.eessiConsumer = eessiConsumer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    public void opprettOgSendSed(long behandlingID, BucType bucType) throws MelosysException {
        opprettOgSendSed(behandlingID, bucType, null);
    }

    public void opprettOgSendSed(long behandlingID, BucType bucType, byte[] vedlegg) throws MelosysException {
        log.info("Starter sending av SED for behandling {}", behandlingID);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (skalSendeSed) {
            Fagsak fagsak = behandling.getFagsak();

            DokumentdataGrunnlag datagrunnlag = dokumentdataGrunnlagFactory.av(behandling);
            SedDataDto sedData = sedDataBygger.lag(datagrunnlag, behandlingsresultat, MedlemsperiodeType.fraBucType(bucType));
            sedData.setGsakSaksnummer(fagsak.getGsakSaksnummer());

            log.info("Oppretter buc og sed for fagsak {}", fagsak.getSaksnummer());
            OpprettSedDto opprettSedDto = eessiConsumer.opprettBucOgSed(sedData, vedlegg, bucType, true);

            log.info("Buc opprettet med id {} for behandling {}", opprettSedDto.getRinaSaksnummer(), behandling.getId());
        }
    }

    @Transactional(readOnly = true)
    public String opprettBucOgSed(Behandling behandling, BucType bucType, String mottakerLand, String mottakerId) throws MelosysException {
        return opprettBucOgSed(behandling, bucType, mottakerLand, mottakerId, null);
    }

    private String opprettBucOgSed(Behandling behandling, BucType bucType, String mottakerLand, String mottakerId, byte[] vedlegg) throws MelosysException {
        if (skalSendeSed) {
            DokumentdataGrunnlag dataGrunnlag = dokumentdataGrunnlagFactory.av(behandling);
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.fraBucType(bucType));
            sedDataDto.setMottakerLand(mottakerLand);
            sedDataDto.setMottakerId(mottakerId);
            sedDataDto.setGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());

            log.info("Oppretter buc og sed for behandling {} med bucType {}", behandling.getId(), bucType);
            return eessiConsumer.opprettBucOgSed(sedDataDto, vedlegg, bucType, false).getRinaUrl();
        }

        throw new IllegalStateException("Ikke mulig å sende sed");
    }

    public List<Institusjon> hentEessiMottakerinstitusjoner(String bucType) throws MelosysException {
        if (skalSendeSed) {
            return eessiConsumer.hentMottakerinstitusjoner(bucType);
        } else {
            return new ArrayList<>();
        }
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

    public Optional<Long> finnSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException {
        return eessiConsumer.hentSakForRinasaksnummer(rinaSaksnummer).stream()
            .findFirst().map(SaksrelasjonDto::getGsakSaksnummer);
    }

    public void lagreSaksrelasjon(Long gsakSaksnummer, String rinaSaksnummer, String bucType) throws MelosysException {
        eessiConsumer.lagreSaksrelasjon(new SaksrelasjonDto(gsakSaksnummer, rinaSaksnummer, bucType));
    }

    public void sendAnmodningUnntakSvar(AnmodningsperiodeSvar anmodningsperiodeSvar, long behandlingId) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        String rinaSaksnummer = SaksopplysningerUtils.hentSedDokument(behandling).getRinaSaksnummer();
        SvarAnmodningUnntakDto svarAnmodningUnntakDto = SvarAnmodningUnntakDto.av(anmodningsperiodeSvar);

        log.info("Sender svar på anmodning om unntak for behandling {}", behandlingId);
        eessiConsumer.sendAnmodningUnntakSvar(svarAnmodningUnntakDto, rinaSaksnummer);
    }

    public byte[] genererSedForhåndsvisning(long behandingID, SedType sedType) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandingID);
        DokumentdataGrunnlag dataGrunnlag = dokumentdataGrunnlagFactory.av(behandling);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandingID);

        MedlemsperiodeType medlemsperiodeType;
        if (sedType == SedType.A001) {
            medlemsperiodeType = MedlemsperiodeType.ANMODNINGSPERIODE;
        } else {
            medlemsperiodeType = MedlemsperiodeType.LOVVALGSPERIODE;
        }

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, medlemsperiodeType);
        log.info("Henter pdf for sed med type {} for behandling {}", sedType, behandingID);
        return eessiConsumer.genererSedForhåndsvisning(sedDataDto, sedType);
    }
}
