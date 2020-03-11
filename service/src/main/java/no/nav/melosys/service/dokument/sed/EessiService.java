package no.nav.melosys.service.dokument.sed;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.MedlemsperiodeType;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.dokument.sed.mapper.SedGrunnlagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Primary
@Service
public class EessiService {
    private static final Logger log = LoggerFactory.getLogger(EessiService.class);

    private final SedDataBygger sedDataBygger;
    private final SedDataGrunnlagFactory dataGrunnlagFactory;
    private final EessiConsumer eessiConsumer;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final boolean skalSendeSed;

    public EessiService(@Value("${MelosysEessi.forsokSendSed:true}") String skalSendeSed, SedDataBygger sedDataBygger,
                        SedDataGrunnlagFactory dataGrunnlagFactory, EessiConsumer eessiConsumer,
                        BehandlingService behandlingService,
                        BehandlingsresultatService behandlingsresultatService) {
        this.skalSendeSed = Boolean.parseBoolean(skalSendeSed);
        this.sedDataBygger = sedDataBygger;
        this.dataGrunnlagFactory = dataGrunnlagFactory;
        this.eessiConsumer = eessiConsumer;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    public void opprettOgSendSed(long behandlingID, List<String> mottakerInstitusjoner, BucType bucType) throws MelosysException {
        opprettOgSendSed(behandlingID, mottakerInstitusjoner, bucType, null);
    }

    public void opprettOgSendSed(long behandlingID, List<String> mottakerInstitusjoner, BucType bucType, byte[] vedlegg) throws MelosysException {
        log.info("Starter sending av SED for behandling {}", behandlingID);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (skalSendeSed) {
            Fagsak fagsak = behandling.getFagsak();

            SedDataGrunnlag datagrunnlag = dataGrunnlagFactory.av(behandling);
            SedDataDto sedData = sedDataBygger.lag(datagrunnlag, behandlingsresultat, MedlemsperiodeType.fraBucType(bucType));
            sedData.setMottakerIder(mottakerInstitusjoner);
            sedData.setGsakSaksnummer(fagsak.getGsakSaksnummer());

            log.info("Oppretter buc og sed for fagsak {}", fagsak.getSaksnummer());
            OpprettSedDto opprettSedDto = eessiConsumer.opprettBucOgSed(sedData, vedlegg, bucType, true);

            log.info("Buc opprettet med id {} for behandling {}", opprettSedDto.getRinaSaksnummer(), behandling.getId());
        }
    }

    @Transactional(readOnly = true)
    public String opprettBucOgSed(Behandling behandling, BucType bucType, List<String> mottakerId) throws MelosysException {
        return opprettBucOgSed(behandling, bucType, mottakerId, null);
    }

    private String opprettBucOgSed(Behandling behandling, BucType bucType, List<String> mottakerId, byte[] vedlegg) throws MelosysException {
        if (skalSendeSed) {
            SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.fraBucType(bucType));
            sedDataDto.setMottakerIder(mottakerId);
            sedDataDto.setGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());

            log.info("Oppretter buc og sed for behandling {} med bucType {}", behandling.getId(), bucType);
            return eessiConsumer.opprettBucOgSed(sedDataDto, vedlegg, bucType, false).getRinaUrl();
        }

        throw new IllegalStateException("Ikke mulig å sende sed");
    }

    public List<Institusjon> hentEessiMottakerinstitusjoner(String bucType, String landkode) throws MelosysException {
        if (skalSendeSed) {
            return eessiConsumer.hentMottakerinstitusjoner(bucType, landkode);
        } else {
            return new ArrayList<>();
        }
    }

    public boolean erGyldigInstitusjonForLand(String bucType, String landkode, String mottakerInstitusjon) throws MelosysException {
        return hentEessiMottakerinstitusjoner(bucType, landkode).stream().anyMatch(l -> l.getId().equals(mottakerInstitusjon));
    }

    public boolean landErEessiReady(String bucType, String landkode) throws MelosysException {
        return !hentEessiMottakerinstitusjoner(bucType, landkode).isEmpty();
    }

    public boolean landErEessiReady(String bucType, Collection<Landkoder> landkoder) throws MelosysException {
        for (Landkoder landkode : landkoder) {
            if (!landErEessiReady(bucType, landkode.getKode())){
                return false;
            }
        }

        return true;
    }

    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) throws MelosysException {
        return eessiConsumer.hentTilknyttedeBucer(gsakSaksnummer, statuser);
    }

    public boolean støtterAutomatiskBehandling(String journalpostID) throws MelosysException {
        return finnBehandlingstypeForSedTilknyttetJournalpost(journalpostID).isPresent();
    }

    public Optional<Behandlingstyper> finnBehandlingstypeForSedTilknyttetJournalpost(String journalpostID) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = hentSedTilknyttetJournalpost(journalpostID);
        String sedType = melosysEessiMelding.getSedType();
        String lovvalgsland = melosysEessiMelding.getLovvalgsland();
        return SedTypeTilBehandlingstypeMapper.finnBehandlingstypeForSedType(sedType, lovvalgsland);
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

    public void sendAnmodningUnntakSvar(long behandlingId) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.ANMODNINGSPERIODE);
        String rinaSaksnummer = SaksopplysningerUtils.hentSedDokument(behandling).getRinaSaksnummer();

        log.info("Sender svar på anmodning om unntak for behandling {}", behandlingId);
        eessiConsumer.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, hentSedTypeForAnmodningUnntakSvar(behandlingsresultat));
    }

    @Transactional(readOnly = true)
    public byte[] genererSedPdf(long behandlingID, SedType sedType) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        MedlemsperiodeType medlemsperiodeType;
        if (sedType == SedType.A001) {
            medlemsperiodeType = MedlemsperiodeType.ANMODNINGSPERIODE;
        } else {
            medlemsperiodeType = MedlemsperiodeType.LOVVALGSPERIODE;
        }

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, medlemsperiodeType);
        log.info("Henter pdf for sed med type {} for behandling {}", sedType, behandlingID);
        return eessiConsumer.genererSedPdf(sedDataDto, sedType);
    }

    public SedType hentSedTypeForAnmodningUnntakSvar(Long behandlingID) throws IkkeFunnetException {
        return hentSedTypeForAnmodningUnntakSvar(behandlingsresultatService.hentBehandlingsresultat(behandlingID));
    }

    private static SedType hentSedTypeForAnmodningUnntakSvar(Behandlingsresultat behandlingsresultat) {
        Anmodningsperiodesvartyper anmodningsperiodeSvarType =
            behandlingsresultat.hentValidertAnmodningsperiode().getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType();

        if (anmodningsperiodeSvarType == Anmodningsperiodesvartyper.INNVILGELSE) {
            return SedType.A011;
        }
        return SedType.A002;
    }

    /**
     * Avklarer om alle land er påkoblet bestemt BUC.
     * Hvis minst et land ikke er påkoblet - returner tom liste. Det skal ikke åpnes BUC med valgte land som mottakere da ikke alle er påkoblet
     * Hvis alle er påkoblet - valider at det er satt nøyaktig èn institusjon for hvert land, returner dermed liste med validerte institusjoner
     */
    public List<String> validerOgAvklarMottakerInstitusjonerForBuc(final List<String> valgteMottakerinstitusjoner, final Collection<Landkoder> mottakerland, BucType bucType) throws MelosysException {

        Map<Landkoder, Collection<String>> institusjonerPerLand = new EnumMap<>(Landkoder.class);

        for (var land : mottakerland) {
            Collection<String> alleInstitusjonerForLand = hentEessiMottakerinstitusjoner(bucType.name(), land.getKode())
                .stream().map(Institusjon::getId).collect(Collectors.toList());
            if (alleInstitusjonerForLand.isEmpty()) {
                log.info("{} er ikke EESSI-ready, skal ikke sendes SED", land.getBeskrivelse());
                return Collections.emptyList();
            }

            institusjonerPerLand.put(land, alleInstitusjonerForLand);
        }

        validerMottakerInstitusjonerForLand(mottakerland, valgteMottakerinstitusjoner, institusjonerPerLand);
        return valgteMottakerinstitusjoner;
    }

    private void validerMottakerInstitusjonerForLand(Collection<Landkoder> mottakerland,
                                                     Collection<String> valgteMottakerinstitusjoner,
                                                     Map<Landkoder, Collection<String>> institusjonerPerLand) throws FunksjonellException {

        List<String> validerteMottakerinstitusjoner = new ArrayList<>();
        StringBuilder feilmelding = new StringBuilder();
        for (var land : mottakerland) {

            Collection<String> alleInstitusjonerForLand = institusjonerPerLand.get(land);
            String validertInstitusjon = CollectionUtils.findFirstMatch(alleInstitusjonerForLand, valgteMottakerinstitusjoner);

            if (validertInstitusjon == null) {
                feilmelding.append("Finner ingen gyldig mottakerinstitusjon for arbeidsland ")
                    .append(land.getBeskrivelse()).append(System.lineSeparator());
            } else {
                validerteMottakerinstitusjoner.add(validertInstitusjon);
            }
        }

        if (feilmelding.length() != 0) {
            throw new FunksjonellException(feilmelding.toString());
        } else if (valgteMottakerinstitusjoner.size() != validerteMottakerinstitusjoner.size()) {
            throw new FunksjonellException("Kan kun velge en mottakerinstitusjon per land. Validerte mottakere: " + validerteMottakerinstitusjoner
                + ". Valgte mottakere " + valgteMottakerinstitusjoner);
        }
    }

    public BehandlingsgrunnlagData hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) throws MelosysException {
        return SedGrunnlagMapper.lagSedGrunnlag(eessiConsumer.hentSedGrunnlag(rinaSaksnummer, rinaDokumentID));
    }
}
