package no.nav.melosys.service.saksflyt;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.metrics.MetrikkerNavn;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.journalforing.dto.DokumentDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.service.soknad.SoknadMottatt;
import no.nav.melosys.service.vedtak.dto.FattFtrlVedtakRequest;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static org.springframework.util.StringUtils.hasText;

@Service
public class ProsessinstansService {
    private static final Logger logger = LoggerFactory.getLogger(ProsessinstansService.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProsessinstansRepository prosessinstansRepo;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    private final Counter prosessinstanserOpprettet = Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET);

    @Autowired
    public ProsessinstansService(ApplicationEventPublisher applicationEventPublisher,
                                 ProsessinstansRepository prosessinstansRepo,
                                 UtenlandskMyndighetService utenlandskMyndighetService,
                                 BehandlingsgrunnlagService behandlingsgrunnlagService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.prosessinstansRepo = prosessinstansRepo;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    public Prosessinstans lagJournalføringProsessinstans(ProsessType type, JournalfoeringDto journalfoeringDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(type);

        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalfoeringDto.getHoveddokument().getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalfoeringDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringDto.getBrukerID());

        prosessinstans.setData(ProsessDataKey.AVSENDER_TYPE, journalfoeringDto.getAvsenderType());
        if (journalfoeringDto.getAvsenderType() == Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET) {
            prosessinstans.setData(ProsessDataKey.AVSENDER_ID, lagInstitusjonsId(journalfoeringDto.getAvsenderID()));
            prosessinstans.setData(ProsessDataKey.AVSENDER_LAND, journalfoeringDto.getAvsenderID());
        } else {
            prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalfoeringDto.getAvsenderID());
        }
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalfoeringDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getHoveddokument().getTittel());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, journalfoeringDto.isSkalTilordnes());
        prosessinstans.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, skalSendesForvaltningsmelding(journalfoeringDto));

        if (journalfoeringDto.getMottattDato() != null) {
            prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, journalfoeringDto.getMottattDato());
        }

        if (!CollectionUtils.isEmpty(journalfoeringDto.getHoveddokument().getLogiskeVedlegg())) {
            prosessinstans.setData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, journalfoeringDto.getHoveddokument().getLogiskeVedlegg());
        }

        if (!CollectionUtils.isEmpty(journalfoeringDto.getVedlegg())) {
            prosessinstans.setData(ProsessDataKey.FYSISKE_VEDLEGG,
                journalfoeringDto.getVedlegg().stream().collect(Collectors.toMap(DokumentDto::getDokumentID, DokumentDto::getTittel)));
        }

        return prosessinstans;
    }

    private String lagInstitusjonsId(String avsenderID) {
        try {
            return utenlandskMyndighetService.lagInstitusjonsId(Landkoder.valueOf(avsenderID));
        } catch (IkkeFunnetException e) {
            logger.warn(e.getMessage());
            logger.warn("Utenlandsk myndighet har ikke institusjonsId. Bruker {}: som avsenderID", avsenderID);
            return avsenderID + ":";
        }
    }

    private static boolean skalSendesForvaltningsmelding(JournalfoeringDto journalfoeringDto) {
        return journalfoeringDto.isIkkeSendForvaltingsmelding() != null && !journalfoeringDto.isIkkeSendForvaltingsmelding();
    }

    public boolean harAktivProsessinstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndStatusIs(
            behandlingID, ProsessStatus.KLAR
        ).isPresent();
    }

    public boolean harVedtakInstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndTypeIn(behandlingID, ProsessType.IVERKSETT_VEDTAK, ProsessType.IVERKSETT_VEDTAK_EOS).isPresent();
    }

    public void lagre(Prosessinstans prosessinstans) {
        lagre(prosessinstans, SubjectHandler.getInstance().getUserID());
    }

    void lagre(Prosessinstans prosessinstans, String saksbehandler) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstans.setStatus(ProsessStatus.KLAR);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        }

        prosessinstansRepo.save(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));
        prosessinstanserOpprettet.increment();

        logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler, prosessinstans.getId(), prosessinstans.getType());
    }

    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling, Set<String> mottakerInstitusjon,
                                                       Set<DokumentReferanse> vedleggReferanserTilSed,
                                                       String ytterligereInformasjonSed) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerInstitusjon)
            .medVedleggTilSed(vedleggReferanserTilSed)
            .medYtterligereinformasjonSed(ytterligereInformasjonSed)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansAnmodningOmUnntakMottakSvar(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR)
            .medBehandling(behandling)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansFagsakHenlagt(Behandling sistAktiveBehandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(sistAktiveBehandling)
            .medType(ProsessType.HENLEGG_SAK)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtakEos(Behandling behandling, Behandlingsresultattyper behandlingsresultatType,
                                                        String fritekst, String fritekstSed, Set<String> mottakerinstitusjoner,
                                                        String revurderBegrunnelse) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_EOS)
            .medBehandling(behandling)
            .medBegrunnelseFritekst(fritekst)
            .medEessiMottakere(mottakerinstitusjoner)
            .medYtterligereinformasjonSed(fritekstSed)
            .build();

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        if (StringUtils.isNotEmpty(revurderBegrunnelse)) {
            prosessinstans.setData(ProsessDataKey.REVURDER_BEGRUNNELSE, revurderBegrunnelse);
        }

        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtak(Behandling behandling, FattFtrlVedtakRequest request) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK)
            .medBehandling(behandling)
            .medBegrunnelseFritekst(request.getFritekstBegrunnelse())
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySak(String journalpostID, OpprettSakDto opprettSakDto, Behandlingstyper behandlingstype) {
        Prosessinstans prosessinstans = new Prosessinstans();

        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK);
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, opprettSakDto.getSakstype());
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, opprettSakDto.getBehandlingstema());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, opprettSakDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, opprettSakDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, opprettSakDto.getSoknadDto().getPeriode());
        prosessinstans.setData(ProsessDataKey.SØKNADSLAND, opprettSakDto.getSoknadDto().getLand());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, opprettSakDto.isSkalTilordnes());

        lagre(prosessinstans);
    }

    public void opprettProsessinstansForkortPeriode(Behandling behandling,
                                                    String fritekst,
                                                    String fritekstSed) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE)
            .medBegrunnelseFritekst(fritekst)
            .medYtterligereinformasjonSed(fritekstSed)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansGodkjennUnntaksperiode(Behandling behandling, boolean varsleUtland) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.REGISTRERING_UNNTAK_GODKJENN)
            .build();

        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, varsleUtland);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansUnntaksperiodeAvvist(Behandling behandling, Collection<Ikke_godkjent_begrunnelser> begrunnelser, String begrunnelseFritekst) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_AVVIS)
            .medBehandling(behandling)
            .medBegrunnelser(begrunnelser)
            .medBegrunnelseFritekst(begrunnelseFritekst)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansOpprettOgDistribuerBrev(Behandling behandling, Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_OG_DISTRIBUER_BREV);
        prosessinstans.setData(BREVBESTILLING, brevbestilling);
        prosessinstans.setData(MOTTAKER, mottaker.getRolle());
        if (hasText(mottaker.getAktørId())) {
            prosessinstans.setData(AKTØR_ID, mottaker.getAktørId());
        }
        if (hasText(mottaker.getOrgnr())) {
            prosessinstans.setData(ORGNR, mottaker.getOrgnr());
        }
        prosessinstans.setBehandling(behandling);
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansSedMottak(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = prosessinstansForSedMottak(melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, melosysEessiMelding.getAktoerId());
        lagre(prosessinstans);
    }

    public void opprettProsessinstansSedMottak(MelosysEessiMelding eessiMelding, String aktørID) {
        Prosessinstans prosessinstans = prosessinstansForSedMottak(eessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        lagre(prosessinstans);
    }

    private Prosessinstans prosessinstansForSedMottak(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.MOTTAK_SED)
            .medEessiMelding(melosysEessiMelding)
            .build();

        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, melosysEessiMelding.getJournalpostId());
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, melosysEessiMelding.getErEndring());
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, melosysEessiMelding.getGsakSaksnummer());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        return prosessinstans;
    }

    public void opprettProsessinstansVideresendSoknad(Behandling behandling, @Nullable String mottakerInstitusjon,
                                                      String fritekstBrev,
                                                      Set<DokumentReferanse> vedleggReferanser) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.VIDERESEND_SOKNAD)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerInstitusjon != null ? Set.of(mottakerInstitusjon) : null)
            .medVedleggTilSed(vedleggReferanser)
            .medBegrunnelseFritekst(fritekstBrev)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansUtpekAnnetLand(Behandling behandling,
                                                    Landkoder utpektLand,
                                                    Set<String> mottakerinstitusjoner,
                                                    String ytterligereInformasjonSed,
                                                    String fritekstBrev) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_EOS)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerinstitusjoner)
            .medYtterligereinformasjonSed(ytterligereInformasjonSed)
            .medBegrunnelseFritekst(fritekstBrev)
            .build();
        prosessinstans.setData(ProsessDataKey.UTPEKT_LAND, utpektLand);
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansSøknadMottatt(SoknadMottatt søknadMottatt) {
        if (behandlingsgrunnlagService.harMottattSøknadMedEksternReferanseID(søknadMottatt.getSoknadID())) {
            logger.warn("Søknad med søknadID {} har vært mottatt tidligere.", søknadMottatt.getSoknadID());
        } else {
            Prosessinstans prosessinstans = new ProsessinstansBuilder()
                .medType(ProsessType.MOTTAK_SOKNAD_ALTINN)
                .build();
            prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, søknadMottatt.getSoknadID());
            prosessinstans.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, true);

            lagre(prosessinstans);
        }
    }

    public void opprettProsessinstansAvvisUtpeking(Behandling behandling, UtpekingAvvis utpekingAvvis) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.UTPEKING_AVVIS)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(ProsessDataKey.UTPEKING_AVVIS, utpekingAvvis);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansSedJournalføring(Behandling behandling, MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.MOTTAK_SED_JOURNALFØRING)
            .medBehandling(behandling)
            .medEessiMelding(melosysEessiMelding)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansMottattSvarAnmodningUnntak(Behandling behandling, MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_SVAR)
            .medBehandling(behandling)
            .medEessiMelding(melosysEessiMelding)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySakUnntaksregistrering(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, String aktørID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_NY_SAK)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNyBehandlingUnntaksregistrering(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, Long arkivsakID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, arkivsakID);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySakArbeidFlereLand(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, String aktørID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ARBEID_FLERE_LAND_NY_SAK)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNyBehandlingArbeidFlereLand(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, Long arkivsakID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, arkivsakID);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySakMottattAnmodningOmUnntak(MelosysEessiMelding eessiMelding, String aktørID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK)
            .medEessiMelding(eessiMelding)
            .build();

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansNyBehandlingMottattAnmodningUnntak(MelosysEessiMelding melosysEessiMelding, Long arkivsakID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, arkivsakID);

        lagre(prosessinstans);
    }
}
