package no.nav.melosys.service.dokument.sed;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.PeriodeType;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.UtpekingAvvisDto;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlag;
import no.nav.melosys.service.eessi.SedGrunnlagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static java.util.function.Predicate.not;

@Primary
@Service
public class EessiService {
    private static final Logger log = LoggerFactory.getLogger(EessiService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final JoarkFasade joarkFasade;
    private final EessiConsumer eessiConsumer;
    private final SedDataBygger sedDataBygger;
    private final SedDataGrunnlagFactory dataGrunnlagFactory;

    public EessiService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                        EessiConsumer eessiConsumer, JoarkFasade joarkFasade,
                        SedDataBygger sedDataBygger, SedDataGrunnlagFactory dataGrunnlagFactory) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.joarkFasade = joarkFasade;
        this.eessiConsumer = eessiConsumer;
        this.sedDataBygger = sedDataBygger;
        this.dataGrunnlagFactory = dataGrunnlagFactory;
    }

    public Collection<Vedlegg> lagEessiVedlegg(Fagsak fagsak, Collection<DokumentReferanse> vedleggReferanser) {
        if (vedleggReferanser.isEmpty()) {
            return Collections.emptySet();
        }
        final List<Journalpost> journalposter = joarkFasade.hentJournalposterTilknyttetSak(
            new HentJournalposterTilknyttetSakRequest(fagsak.getGsakSaksnummer(), fagsak.getSaksnummer())
        );
        final Collection<Vedlegg> vedlegg = new ArrayList<>();
        for (DokumentReferanse dokumentReferanse : vedleggReferanser) {
            Journalpost journalpost = journalposter.stream()
                .filter(jp -> jp.getJournalpostId().equals(dokumentReferanse.getJournalpostID()))
                .findFirst()
                .orElseThrow(() -> new IkkeFunnetException(String.format("Finner ikke journalpost %s for sak %s",
                        dokumentReferanse.getJournalpostID(), fagsak.getSaksnummer())));
            vedlegg.add(lagEessiVedlegg(journalpost, dokumentReferanse));
        }
        return vedlegg;
    }

    private Vedlegg lagEessiVedlegg(Journalpost journalpost, DokumentReferanse vedleggReferanse) {
        byte[] pdf = joarkFasade.hentDokument(vedleggReferanse.getJournalpostID(), vedleggReferanse.getDokumentID());
        String tittel = journalpost.hentArkivDokument(vedleggReferanse.getDokumentID()).getTittel();
        return new Vedlegg(pdf, tittel);
    }

    public void opprettOgSendSed(long behandlingID, List<String> mottakerInstitusjoner, BucType bucType,
                                 Collection<Vedlegg> vedlegg, String ytterligereInformasjon) {
        log.info("Starter sending av SED for behandling {}", behandlingID);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        Fagsak fagsak = behandling.getFagsak();

        SedDataGrunnlag datagrunnlag = dataGrunnlagFactory.av(behandling);
        SedDataDto sedData = sedDataBygger.lag(datagrunnlag, behandlingsresultat, PeriodeType.fraBucType(bucType, behandlingsresultat));
        sedData.setMottakerIder(mottakerInstitusjoner);
        sedData.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        sedData.setYtterligereInformasjon(ytterligereInformasjon);

        log.info("Oppretter buc og sed for fagsak {}", fagsak.getSaksnummer());
        OpprettSedDto opprettSedDto = eessiConsumer.opprettBucOgSed(
            sedData,
            vedlegg,
            bucType,
            true,
            true);

        log.info("Buc opprettet med id {} for behandling {}", opprettSedDto.getRinaSaksnummer(), behandling.getId());
    }

    @Transactional(readOnly = true)
    public String opprettBucOgSed(Behandling behandling,
                                  BucType bucType,
                                  List<String> mottakerInstitusjoner,
                                  Collection<DokumentReferanse> vedleggReferanser) {
        SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, PeriodeType.fraBucType(bucType, behandlingsresultat));
        sedDataDto.setMottakerIder(mottakerInstitusjoner);
        sedDataDto.setGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());

        final var vedlegg = lagEessiVedlegg(behandling.getFagsak(), vedleggReferanser);
        log.info("Oppretter buc og sed for behandling {} med bucType {}", behandling.getId(), bucType);
        return eessiConsumer.opprettBucOgSed(sedDataDto, vedlegg, bucType, false, false).getRinaUrl();
    }

    public List<Institusjon> hentEessiMottakerinstitusjoner(String bucType, Collection<String> landkoder) {
        return eessiConsumer.hentMottakerinstitusjoner(bucType, landkoder);
    }

    private boolean landErEessiReady(String bucType, String landkode) {
        return !hentEessiMottakerinstitusjoner(bucType, Set.of(landkode)).isEmpty();
    }

    public boolean landErEessiReady(String bucType, Collection<Landkoder> landkoder) {
        for (Landkoder landkode : landkoder) {
            if (!landErEessiReady(bucType, landkode.getKode())) {
                return false;
            }
        }

        return true;
    }

    public List<BucInformasjon> hentTilknyttedeBucer(long arkivsakID, List<String> statuser) {
        return eessiConsumer.hentTilknyttedeBucer(arkivsakID, statuser);
    }

    public boolean støtterAutomatiskBehandling(String journalpostID) {
        return finnBehandlingstemaForSedTilknyttetJournalpost(hentSedTilknyttetJournalpost(journalpostID)).isPresent();
    }

    public boolean støtterAutomatiskBehandling(MelosysEessiMelding melosysEessiMelding) {
        return finnBehandlingstemaForSedTilknyttetJournalpost(melosysEessiMelding).isPresent();
    }

    public Optional<Behandlingstema> finnBehandlingstemaForSedTilknyttetJournalpost(String journalpostID) {
        return finnBehandlingstemaForSedTilknyttetJournalpost(hentSedTilknyttetJournalpost(journalpostID));

    }

    private Optional<Behandlingstema> finnBehandlingstemaForSedTilknyttetJournalpost(MelosysEessiMelding melosysEessiMelding) {
        String sedType = melosysEessiMelding.getSedType();
        String lovvalgsland = melosysEessiMelding.getLovvalgsland();
        return SedTypeTilBehandlingstemaMapper.finnBehandlingstemaForSedType(sedType, lovvalgsland);
    }

    public MelosysEessiMelding hentSedTilknyttetJournalpost(String journalpostID) {
        return eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(journalpostID);
    }

    public Optional<Long> finnSakForRinasaksnummer(String rinaSaksnummer) {
        return eessiConsumer.hentSakForRinasaksnummer(rinaSaksnummer).stream()
            .findFirst().map(SaksrelasjonDto::getGsakSaksnummer);
    }

    public void lagreSaksrelasjon(Long arkivsakID, String rinaSaksnummer, String bucType) {
        log.info("Lagrer saksrelasjon mellom arkivsak {} og rinasak {}", arkivsakID, rinaSaksnummer);
        eessiConsumer.lagreSaksrelasjon(new SaksrelasjonDto(arkivsakID, rinaSaksnummer, bucType));
    }

    public void sendAnmodningUnntakSvar(long behandlingId) {
        log.info("Sender svar på anmodning om unntak for behandling {}", behandlingId);
        sendSedPåEksisterendeBehandling(behandlingId, PeriodeType.ANMODNINGSPERIODE, this::hentSedTypeForAnmodningUnntakSvar);

    }

    public void sendGodkjenningArbeidFlereLand(long behandlingID, String ytterligereInformasjon) {
        log.info("Sender svar på A003 for behandling {}", behandlingID);
        sendSedPåEksisterendeBehandling(behandlingID, PeriodeType.LOVVALGSPERIODE, br -> SedType.A012, ytterligereInformasjon);
    }

    private void sendSedPåEksisterendeBehandling(long behandlingID,
                                                 PeriodeType periodeType,
                                                 Function<Behandlingsresultat, SedType> sedTypeAvklarer) {
        sendSedPåEksisterendeBehandling(behandlingID, periodeType, sedTypeAvklarer, null);
    }

    private void sendSedPåEksisterendeBehandling(long behandlingID,
                                                 PeriodeType periodeType,
                                                 Function<Behandlingsresultat, SedType> sedTypeAvklarer, String ytterligereInformasjon) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, periodeType);
        sedDataDto.setYtterligereInformasjon(ytterligereInformasjon);
        String rinaSaksnummer = behandling.hentSedDokument().getRinaSaksnummer();

        log.info("Sender svar på anmodning om unntak for behandling {}", behandlingID);
        eessiConsumer.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, sedTypeAvklarer.apply(behandlingsresultat));
    }

    public void sendAvslagUtpekingSvar(long behandlingId, UtpekingAvvis utpekingAvvis) {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId);
        SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);

        String rinaSaksnummer = behandling.hentSedDokument().getRinaSaksnummer();

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);
        sedDataDto.setYtterligereInformasjon(utpekingAvvis.getFritekst());
        sedDataDto.setUtpekingAvvis(new UtpekingAvvisDto(
            utpekingAvvis.getNyttLovvalgsland(),
            utpekingAvvis.getBegrunnelse(),
            utpekingAvvis.isEtterspørInformasjon()
        ));
        eessiConsumer.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, SedType.A004);
    }

    @Transactional(readOnly = true)
    public byte[] genererSedPdf(long behandlingID, SedType sedType) {
        return genererSedPdf(behandlingID, sedType, null);
    }

    @Transactional(readOnly = true)
    public byte[] genererSedPdf(long behandlingID, SedType sedType, @Nullable SedPdfData sedPdfData) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        SedDataGrunnlag dataGrunnlag = dataGrunnlagFactory.av(behandling);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        PeriodeType periodeType;
        if (sedType == SedType.A001) {
            periodeType = PeriodeType.ANMODNINGSPERIODE;
        } else if (sedType == SedType.A003 && behandlingsresultat.finnValidertUtpekingsperiode().isPresent()) {
            periodeType = PeriodeType.UTPEKINGSPERIODE;
        } else {
            periodeType = PeriodeType.LOVVALGSPERIODE;
        }

        SedDataDto sedDataDto = sedDataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, periodeType);

        if (sedPdfData != null) {
            sedPdfData.utfyllSedDataDto(sedDataDto);
        }
        log.info("Henter pdf for sed med type {} for behandling {}", sedType, behandlingID);
        return eessiConsumer.genererSedPdf(sedDataDto, sedType);
    }

    public SedType hentSedTypeForAnmodningUnntakSvar(Long behandlingID) {
        return hentSedTypeForAnmodningUnntakSvar(behandlingsresultatService.hentBehandlingsresultat(behandlingID));
    }

    private SedType hentSedTypeForAnmodningUnntakSvar(Behandlingsresultat behandlingsresultat) {
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
    public Set<String> validerOgAvklarMottakerInstitusjonerForBuc(final Set<String> valgteMottakerinstitusjoner, final Collection<Landkoder> mottakerland, BucType bucType) {

        Set<String> landkoder = mottakerland.stream().map(Landkoder::getKode).collect(Collectors.toSet());
        Map<Landkoder, Set<String>> institusjonerPerLand = hentEessiMottakerinstitusjonerPerLand(bucType, landkoder);

        if (institusjonerPerLand.keySet().size() < mottakerland.size()) {
            log.info("{} er ikke EESSI-ready, skal ikke sendes SED", mottakerland.stream()
                .filter(not(institusjonerPerLand::containsKey))
                .map(Landkoder::getBeskrivelse)
                .collect(Collectors.joining(", ")));
            return Collections.emptySet();
        }

        validerMottakerInstitusjonerForLand(mottakerland, valgteMottakerinstitusjoner, institusjonerPerLand);
        return valgteMottakerinstitusjoner;
    }

    private Map<Landkoder, Set<String>> hentEessiMottakerinstitusjonerPerLand(BucType bucType, Set<String> landkoder) {
        return hentEessiMottakerinstitusjoner(bucType.name(), landkoder).stream()
            .collect(Collectors.groupingBy(
                institusjon -> Landkoder.valueOf(institusjon.getLandkode()),
                Collectors.mapping(Institusjon::getId, Collectors.toSet())));
    }

    private void validerMottakerInstitusjonerForLand(Collection<Landkoder> mottakerland,
                                                     Collection<String> valgteMottakerinstitusjoner,
                                                     Map<Landkoder, Set<String>> institusjonerPerLand) {

        List<String> validerteMottakerinstitusjoner = new ArrayList<>();
        StringBuilder feilmelding = new StringBuilder();
        for (var land : mottakerland) {

            Set<String> alleInstitusjonerForLand = institusjonerPerLand.get(land);
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

    public SedGrunnlag hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) {
        return SedGrunnlagMapper.tilSedGrunnlag(eessiConsumer.hentSedGrunnlag(rinaSaksnummer, rinaDokumentID));
    }
}
