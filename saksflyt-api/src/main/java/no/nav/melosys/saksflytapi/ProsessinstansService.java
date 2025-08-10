package no.nav.melosys.saksflytapi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.Nullable;
import no.nav.melosys.config.MDCOperations;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.metrics.MetrikkerNavn;
import no.nav.melosys.saksflytapi.domain.*;
import no.nav.melosys.saksflytapi.journalfoering.*;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.*;
import static org.springframework.util.StringUtils.hasText;

@Service
public class ProsessinstansService {
    private static final Logger logger = LoggerFactory.getLogger(ProsessinstansService.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProsessinstansForServiceRepository prosessinstansRepo;
    private final ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor;

    public ProsessinstansService(ApplicationEventPublisher applicationEventPublisher,
                                 ProsessinstansForServiceRepository prosessinstansRepo,
                                 @Qualifier("saksflytThreadPoolTaskExecutor") ThreadPoolTaskExecutor saksflytThreadPoolTaskExecutor) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.prosessinstansRepo = prosessinstansRepo;
        this.saksflytThreadPoolTaskExecutor = saksflytThreadPoolTaskExecutor;
    }

    @Transactional
    public void opprettNySakOgBehandling(OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(SAKSTYPE, opprettSakRequest.getSakstype())
            .medData(SAKSTEMA, opprettSakRequest.getSakstema())
            .medData(BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema())
            .medData(BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype())
            .medData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType())
            .medData(BEHANDLINGSÅRSAK_FRITEKST, opprettSakRequest.getBehandlingsaarsakFritekst())
            .medData(BRUKER_ID, opprettSakRequest.getBrukerID())
            .medData(VIRKSOMHET_ORGNR, opprettSakRequest.getVirksomhetOrgnr())
            .medData(MOTTATT_DATO, opprettSakRequest.getMottaksdato())
            .medData(SØKNADSLAND, opprettSakRequest.getSoknad().getLand())
            .medData(SØKNADSPERIODE, opprettSakRequest.getSoknad().getPeriode())
            .medData(SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes())
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettOgReplikerBehandlingForSak(String saksnummer, OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(SAKSNUMMER, saksnummer)
            .medData(BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema())
            .medData(BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype())
            .medData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType())
            .medData(BEHANDLINGSÅRSAK_FRITEKST, opprettSakRequest.getBehandlingsaarsakFritekst())
            .medData(MOTTATT_DATO, opprettSakRequest.getMottaksdato())
            .medData(SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes())
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettNyBehandlingForSak(String saksnummer, OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_NY_BEHANDLING_FOR_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(SAKSNUMMER, saksnummer)
            .medData(BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema())
            .medData(BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype())
            .medData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType())
            .medData(BEHANDLINGSÅRSAK_FRITEKST, opprettSakRequest.getBehandlingsaarsakFritekst())
            .medData(MOTTATT_DATO, opprettSakRequest.getMottaksdato())
            .medData(SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes())
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettProsessManglendeInnbetalingBehandling(ManglendeFakturabetalingMelding manglendeFakturabetalingMelding) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING)
            .medStatus(ProsessStatus.KLAR)
            .medData(MOTTATT_DATO, manglendeFakturabetalingMelding.getDatoMottatt())
            .medData(FAKTURASERIE_REFERANSE, manglendeFakturabetalingMelding.getFakturaserieReferanse())
            .medData(BETALINGSSTATUS, manglendeFakturabetalingMelding.getBetalingsstatus())
            .medData(FAKTURANUMMER, manglendeFakturabetalingMelding.getFakturanummer())
            .medLåsReferanse(LåsReferanseFactory.lagString(manglendeFakturabetalingMelding))
            .build();

        return lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettProsessManglendeInnbetalingVarselBrev(Behandling behandling, ManglendeFakturabetalingMelding manglendeFakturabetalingMelding) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.MANGLENDE_INNBETALING_VARSELBREV)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(MOTTATT_DATO, manglendeFakturabetalingMelding.getDatoMottatt())
            .medData(BETALINGSSTATUS, manglendeFakturabetalingMelding.getBetalingsstatus())
            .medData(FAKTURANUMMER, manglendeFakturabetalingMelding.getFakturanummer())
            .build();

        return lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettArsavregningsBehandlingProsessflyt(String saksnummer, String gjelderPeriode) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING)
            .medStatus(ProsessStatus.KLAR)
            .medData(GJELDER_ÅR, gjelderPeriode)
            .medData(SAKSNUMMER, saksnummer)
            .build();

        return lagre(prosessinstans);
    }

    @Transactional
    public void opprettAnnullerFagsakProsessflyt(Behandling behandling) {
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.ANNULLER_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(ProsessDataKey.SAKSSTATUS, Saksstatuser.ANNULLERT)
            .build();
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansJournalføringKnyttTilEksisterende(JournalfoeringTilordneRequest journalfoeringRequest, String saksnummer,
                                                                       Fagsak fagsak, String institusjonID, boolean mottaksKanalErElektronisk) {
        Prosessinstans prosessinstans = lagJournalføringProsessinstans(ProsessType.JFR_KNYTT, journalfoeringRequest, institusjonID, mottaksKanalErElektronisk);
        prosessinstans.setBehandling(fagsak.hentSistAktivBehandlingIkkeÅrsavregning());
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringRequest.getIngenVurdering());

        lagre(prosessinstans);
    }

    @Transactional
    public void journalførOgOpprettAndregangsBehandling(ProsessType prosessTypeForAndregangsbehandling, Behandlingstema behandlingstema,
                                                        Behandlingstyper behandlingstype, JournalfoeringTilordneRequest journalfoeringRequest,
                                                        Behandlingsaarsaktyper behandlingsaarsaktyper, LocalDate mottaksdato, String institusjonID,
                                                        boolean mottaksKanalErElektronisk) {
        Prosessinstans prosessinstans = lagJournalføringProsessinstans(prosessTypeForAndregangsbehandling, journalfoeringRequest, institusjonID, mottaksKanalErElektronisk);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, behandlingsaarsaktyper);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, mottaksdato);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, journalfoeringRequest.getSaksnummer());

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansJournalføringNySak(JournalfoeringOpprettRequest journalfoeringRequest, ProsessType prosessType,
                                                        boolean skalSetteSøknadslandOgPeriode, LocalDate mottaksdato,
                                                        Behandlingsaarsaktyper behandlingsaarsaktype, String institusjonID,
                                                        boolean mottaksKanalErElektronisk) {
        Prosessinstans prosessinstans = lagJournalføringProsessinstans(prosessType, journalfoeringRequest, institusjonID, mottaksKanalErElektronisk);
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, Sakstyper.valueOf(journalfoeringRequest.getFagsak().getSakstype()));
        prosessinstans.setData(ProsessDataKey.SAKSTEMA, Sakstemaer.valueOf(journalfoeringRequest.getFagsak().getSakstema()));
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.valueOf(journalfoeringRequest.getBehandlingstypeKode()));
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, behandlingsaarsaktype);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, mottaksdato);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.valueOf(journalfoeringRequest.getBehandlingstemaKode()));

        if (skalSetteSøknadslandOgPeriode) {
            prosessinstans.setData(ProsessDataKey.SØKNADSLAND, journalfoeringRequest.getFagsak().getLand());
            prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalfoeringRequest.getFagsak().getSoknadsperiode());
        }

        lagre(prosessinstans);
    }

    Prosessinstans lagJournalføringProsessinstans(ProsessType type, JournalfoeringRequest journalfoeringRequest, String institusjonID, boolean mottaksKanalErElektronisk) {
        Prosessinstans.Builder builder = Prosessinstans.builder()
            .medType(type)
            .medStatus(ProsessStatus.KLAR)
            .medData(ProsessDataKey.JOURNALPOST_ID, journalfoeringRequest.getJournalpostID())
            .medData(ProsessDataKey.DOKUMENT_ID, journalfoeringRequest.getHoveddokument().getDokumentID())
            .medData(ProsessDataKey.OPPGAVE_ID, journalfoeringRequest.getOppgaveID())
            .medData(ProsessDataKey.BRUKER_ID, journalfoeringRequest.getBrukerID())
            .medData(ProsessDataKey.VIRKSOMHET_ORGNR, journalfoeringRequest.getVirksomhetOrgnr())
            .medData(ProsessDataKey.MOTTAKSKANAL_ER_ELEKTRONISK, mottaksKanalErElektronisk)
            .medData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringRequest.getHoveddokument().getTittel())
            .medData(ProsessDataKey.SKAL_TILORDNES, journalfoeringRequest.getSkalTilordnes())
            .medData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, journalfoeringRequest.getForvaltningsmeldingMottaker());

        if (!mottaksKanalErElektronisk) {
            builder.medData(ProsessDataKey.AVSENDER_TYPE, journalfoeringRequest.getAvsenderType());
            if (journalfoeringRequest.getAvsenderType() == Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET) {
                builder.medData(ProsessDataKey.AVSENDER_ID, institusjonID);
                builder.medData(ProsessDataKey.AVSENDER_LAND, journalfoeringRequest.getAvsenderID());
            } else {
                builder.medData(ProsessDataKey.AVSENDER_ID, journalfoeringRequest.getAvsenderID());
            }
            builder.medData(ProsessDataKey.AVSENDER_NAVN, journalfoeringRequest.getAvsenderNavn());
        }

        if (journalfoeringRequest.getMottattDato() != null) {
            builder.medData(ProsessDataKey.MOTTATT_DATO, journalfoeringRequest.getMottattDato());
        }

        if (!CollectionUtils.isEmpty(journalfoeringRequest.getHoveddokument().getLogiskeVedlegg())) {
            builder.medData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, journalfoeringRequest.getHoveddokument().getLogiskeVedlegg());
        }

        if (!CollectionUtils.isEmpty(journalfoeringRequest.getVedlegg())) {
            builder.medData(ProsessDataKey.FYSISKE_VEDLEGG,
                journalfoeringRequest.getVedlegg().stream().collect(Collectors.toMap(DokumentRequest::getDokumentID, DokumentRequest::getTittel)));
        }

        return builder.build();
    }

    private static String getSaksbehandlerIdent() {
        String saksbehandlerIdent = SubjectHandler.getInstance().getUserID();
        if (saksbehandlerIdent != null) return saksbehandlerIdent;

        //Når en prosess lager en ny prosess har vi ingen innlogget bruker
        return ThreadLocalAccessInfo.getSaksbehandler();
    }

    private static String getSaksbehandlerNavn() {
        String saksbehandlerNavn = SubjectHandler.getInstance().getUserName();
        if (saksbehandlerNavn != null) return saksbehandlerNavn;

        //Når en prosess lager en ny prosess har vi ingen innlogget bruker
        return ThreadLocalAccessInfo.getSaksbehandlerNavn();
    }

    public boolean harVedtakInstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndTypeIn(behandlingID,
            ProsessType.IVERKSETT_VEDTAK_FTRL,
            ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE,
            ProsessType.IVERKSETT_VEDTAK_EOS).isPresent();
    }

    UUID lagre(Prosessinstans prosessinstans) {
        return lagre(prosessinstans, getSaksbehandlerIdent(), getSaksbehandlerNavn());
    }

    UUID lagre(Prosessinstans prosessinstans, String saksbehandler, String saksbehandlerNavn) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstans.setStatus(ProsessStatus.KLAR);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER_NAVN, saksbehandlerNavn);
        }
        prosessinstans.setData(CORRELATION_ID_SAKSFLYT, MDCOperations.getCorrelationId());
        prosessinstans.setData(PROCESS_PARENT_ID, ThreadLocalAccessInfo.getProcessId());

        prosessinstansRepo.save(prosessinstans);
        if (saksbehandler != null) {
            logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler,
                prosessinstans.getId(), prosessinstans.getType());
        } else {
            logger.info("Melosys har opprettet prosessinstans {} av type {}.", prosessinstans.getId(),
                prosessinstans.getType());
        }

        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));
        int prosessinstanseriKø = saksflytThreadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size();
        if (prosessinstanseriKø > 0)
            logger.info("Antall prosessinstanser i saksflytThreadPoolTaskExecutor kø: {}", prosessinstanseriKø);

        Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET, MetrikkerNavn.TAG_TYPE, prosessinstans.getType().name()).increment();
        return prosessinstans.getId();
    }

    @Transactional
    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling, Set<String> mottakerInstitusjon,
                                                       Set<DokumentReferanse> vedleggReferanserTilSed,
                                                       String ytterligereInformasjonSed, String begrunnelseFritekst) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerInstitusjon)
            .medVedleggTilSed(vedleggReferanserTilSed)
            .medYtterligereinformasjonSed(ytterligereInformasjonSed)
            .medBegrunnelseFritekst(begrunnelseFritekst)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansAnmodningOmUnntakMottakSvar(Behandling behandling, String ytterligereInfo) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR)
            .medBehandling(behandling)
            .medYtterligereinformasjonSed(ytterligereInfo)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansFagsakHenlagt(Behandling sistAktiveBehandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(sistAktiveBehandling)
            .medType(ProsessType.HENLEGG_SAK)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansIverksettVedtakEos(Behandling behandling, Behandlingsresultattyper behandlingsresultatType,
                                                        String fritekst, String fritekstSed, Set<String> mottakerinstitusjoner,
                                                        boolean arbeidsgiverSkalHaKopi) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_EOS)
            .medBehandling(behandling)
            .medBegrunnelseFritekst(fritekst)
            .medEessiMottakere(mottakerinstitusjoner)
            .medYtterligereinformasjonSed(fritekstSed)
            .build();

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        prosessinstans.setData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VEDTAK);
        prosessinstans.setData(ARBEIDSGIVER_SKAL_HA_KOPI, arbeidsgiverSkalHaKopi);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansIverksettVedtakFTRL(Behandling behandling, VedtakRequest request, Saksstatuser saksstatus) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medBehandling(behandling)
            .build();

        prosessinstans.setData(SAKSSTATUS, saksstatus);
        prosessinstans.setData(BETALINGSINTERVALL, request.getBetalingsintervall());

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansIverksettVedtakTrygdeavtale(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE)
            .medBehandling(behandling)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansIverksettVedtakÅrsavregning(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING)
            .medBehandling(behandling)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansRegistrerUnntakFraMedlemskap(Behandling behandling, Saksstatuser saksstatus) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.REGISTRERE_UNNTAK_FRA_MEDLEMSKAP)
            .build();

        prosessinstans.setData(SAKSSTATUS, saksstatus);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansEøsPensjonistAvgift(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.IVERKSETT_EOS_PENSJONIST_AVGIFT)
            .build();

        prosessinstans.setData(SAKSSTATUS, Saksstatuser.TRYGDEAVGIFT_AVKLART);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansIverksettIkkeYrkesaktiv(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV)
            .build();

        prosessinstans.setData(SAKSSTATUS, Saksstatuser.LOVVALG_AVKLART);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansGodkjennUnntaksperiode(Behandling behandling, boolean varsleUtland, String fritekst, MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.REGISTRERING_UNNTAK_GODKJENN)
            .medYtterligereinformasjonSed(fritekst)
            .medEessiMelding(melosysEessiMelding)
            .build();

        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, varsleUtland);
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansUnntaksperiodeAvvist(Behandling behandling, String begrunnelseFritekst) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_AVVIS)
            .medBehandling(behandling)
            .medBegrunnelseFritekst(begrunnelseFritekst)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansOpprettOgDistribuerBrev(Behandling behandling, Mottaker mottaker, DokgenBrevbestilling brevbestilling) {
        Prosessinstans.Builder builder = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(BREVBESTILLING, brevbestilling)
            .medData(MOTTAKER, mottaker.getRolle());

        if (hasText(mottaker.getAktørId())) {
            builder.medData(AKTØR_ID, mottaker.getAktørId());
        }
        if (hasText(mottaker.getPersonIdent())) {
            builder.medData(PERSON_IDENT, mottaker.getPersonIdent());
        }
        if (hasText(mottaker.getOrgnr())) {
            builder.medData(ORGNR, mottaker.getOrgnr());
        }
        if (hasText(mottaker.getInstitusjonID())) {
            builder.medData(INSTITUSJON_ID, String.format("\"%s\"", mottaker.getInstitusjonID()));
        }

        Prosessinstans prosessinstans = builder.build();
        lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettProsessinstansSedMottak(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = prosessinstansForSedMottak(melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, melosysEessiMelding.getAktoerId());
        return lagre(prosessinstans);
    }

    @Transactional
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

    @Transactional
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

        prosessinstans.setData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VIKTIG);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansUtpekAnnetLand(Behandling behandling,
                                                    Land_iso2 utpektLand,
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
        prosessinstans.setData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VEDTAK);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansSøknadMottatt(String søknadID, boolean erSøknadMottatTidligere, boolean erSøknadForGammelTilForvaltningsmelding) {
        if (erSøknadMottatTidligere) {
            logger.warn("Søknad med søknadID {} har vært mottatt tidligere.", søknadID);
        } else {
            Prosessinstans prosessinstans = new ProsessinstansBuilder()
                .medType(ProsessType.MOTTAK_SOKNAD_ALTINN)
                .build();
            prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, søknadID);
            prosessinstans.setData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, erSøknadForGammelTilForvaltningsmelding ?
                ForvaltningsmeldingMottaker.INGEN : ForvaltningsmeldingMottaker.BRUKER);

            lagre(prosessinstans);
        }
    }

    @Transactional
    public void opprettProsessinstansAvvisUtpeking(Behandling behandling, UtpekingAvvis utpekingAvvis) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.UTPEKING_AVVIS)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(ProsessDataKey.UTPEKING_AVVIS, utpekingAvvis);

        lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettProsessinstansSedJournalføring(Behandling behandling, MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.MOTTAK_SED_JOURNALFØRING)
            .medBehandling(behandling)
            .medEessiMelding(melosysEessiMelding)
            .build();

        return lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansMottattSvarAnmodningUnntak(Behandling behandling, MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_SVAR)
            .medBehandling(behandling)
            .medEessiMelding(melosysEessiMelding)
            .build();

        lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettProsessinstansNySakUnntaksregistrering(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, String aktørID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_NY_SAK)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(SAKSTEMA, Sakstemaer.UNNTAK);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        return lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansNyBehandlingUnntaksregistrering(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, Long arkivsakID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_NY_BEHANDLING)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, arkivsakID);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansNySakArbeidFlereLand(MelosysEessiMelding melosysEessiMelding, Sakstemaer sakstema,
                                                          Behandlingstema behandlingstema, String aktørID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ARBEID_FLERE_LAND_NY_SAK)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(SAKSTEMA, sakstema);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansNyBehandlingArbeidFlereLand(MelosysEessiMelding melosysEessiMelding, Behandlingstema behandlingstema, Long arkivsakID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ARBEID_FLERE_LAND_NY_BEHANDLING)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, arkivsakID);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansNySakMottattAnmodningOmUnntak(MelosysEessiMelding eessiMelding, String aktørID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_SAK)
            .medEessiMelding(eessiMelding)
            .build();

        prosessinstans.setData(SAKSTEMA, Sakstemaer.UNNTAK);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansNyBehandlingMottattAnmodningUnntak(MelosysEessiMelding melosysEessiMelding, Long arkivsakID) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_NY_BEHANDLING)
            .medEessiMelding(melosysEessiMelding)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, arkivsakID);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstanserSendBrev(Behandling behandling, DoksysBrevbestilling brevbestilling, List<Mottaker> mottakere) {
        for (Mottaker mottaker : mottakere) {
            prosessForSendBrev(behandling, brevbestilling, mottaker);
        }
    }

    @Transactional
    public void opprettProsessinstansSendBrev(Behandling behandling, DoksysBrevbestilling brevbestilling, Mottaker mottaker) {
        prosessForSendBrev(behandling, brevbestilling, mottaker);
    }

    private UUID prosessForSendBrev(Behandling behandling, DoksysBrevbestilling brevbestilling, Mottaker mottaker) {
        brevbestilling.settMottaker(mottaker);

        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.SEND_BREV)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(BREVBESTILLING, brevbestilling);

        return lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansOppdaterFaktura(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.OPPDATER_FAKTURAMOTTAKER)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(SAKSNUMMER, behandling.getFagsak().getSaksnummer());

        lagre(prosessinstans);
    }

    @Transactional
    public UUID opprettSatsendringBehandlingFor(Behandling behandling, int aar) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.SATSENDRING)
            .build();

        prosessinstans.setData(OPPRINNELIG_BEH, behandling.getId());
        prosessinstans.setData(GJELDER_ÅR, aar);

        if (harPågåendeProsess(behandling.getId())) {
            throw new FunksjonellException("Det finnes allerede en aktiv prosess for satsendring av behandling " + behandling.getId());
        } else {
            return lagre(prosessinstans);
        }
    }

    @Transactional
    public UUID opprettSatsendringBehandlingMedTilbakestillingAvAvgift(Behandling behandling, int aar) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.SATSENDRING_TILBAKESTILL_NY_VURDERING)
            .build();
        prosessinstans.setData(OPPRINNELIG_BEH, behandling.getId());
        prosessinstans.setData(GJELDER_ÅR, aar);

        if (harPågåendeProsess(behandling.getId())) {
            throw new FunksjonellException("Det finnes allerede en aktiv prosess for satsendring av behandling " + behandling.getId());
        } else {
            return lagre(prosessinstans);
        }
    }

    private boolean harPågåendeProsess(Long behandlingID) {
        return !prosessinstansRepo.findBySatsendringAndOpprinneligBehandlingIdNotFerdig(behandlingID).isEmpty();
    }
}
