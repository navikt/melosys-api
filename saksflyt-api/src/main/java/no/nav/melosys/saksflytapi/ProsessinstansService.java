package no.nav.melosys.saksflytapi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
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
import no.nav.melosys.metrics.MetrikkerNavn;
import no.nav.melosys.saksflytapi.domain.*;
import no.nav.melosys.saksflytapi.journalfoering.*;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

    private final Counter prosessinstanserOpprettet = Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET);

    public ProsessinstansService(ApplicationEventPublisher applicationEventPublisher,
                                 ProsessinstansForServiceRepository prosessinstansRepo) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    public void opprettNySakOgBehandling(OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_SAK);

        prosessinstans.setData(SAKSTYPE, opprettSakRequest.getSakstype());
        prosessinstans.setData(SAKSTEMA, opprettSakRequest.getSakstema());
        prosessinstans.setData(BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema());
        prosessinstans.setData(BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype());
        prosessinstans.setData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType());
        prosessinstans.setData(BEHANDLINGSÅRSAK_FRITEKST, opprettSakRequest.getBehandlingsaarsakFritekst());
        prosessinstans.setData(BRUKER_ID, opprettSakRequest.getBrukerID());
        prosessinstans.setData(VIRKSOMHET_ORGNR, opprettSakRequest.getVirksomhetOrgnr());
        prosessinstans.setData(MOTTATT_DATO, opprettSakRequest.getMottaksdato());
        prosessinstans.setData(SØKNADSLAND, opprettSakRequest.getSoknad().getLand());
        prosessinstans.setData(SØKNADSPERIODE, opprettSakRequest.getSoknad().getPeriode());
        prosessinstans.setData(SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes());

        lagre(prosessinstans);
    }

    public void opprettOgReplikerBehandlingForSak(String saksnummer, OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK);

        prosessinstans.setData(SAKSNUMMER, saksnummer);
        prosessinstans.setData(BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema());
        prosessinstans.setData(BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype());
        prosessinstans.setData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType());
        prosessinstans.setData(BEHANDLINGSÅRSAK_FRITEKST, opprettSakRequest.getBehandlingsaarsakFritekst());
        prosessinstans.setData(MOTTATT_DATO, opprettSakRequest.getMottaksdato());
        prosessinstans.setData(SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes());

        lagre(prosessinstans);
    }

    public void opprettNyBehandlingForSak(String saksnummer, OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_NY_BEHANDLING_FOR_SAK);

        prosessinstans.setData(SAKSNUMMER, saksnummer);
        prosessinstans.setData(BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema());
        prosessinstans.setData(BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype());
        prosessinstans.setData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType());
        prosessinstans.setData(BEHANDLINGSÅRSAK_FRITEKST, opprettSakRequest.getBehandlingsaarsakFritekst());
        prosessinstans.setData(MOTTATT_DATO, opprettSakRequest.getMottaksdato());
        prosessinstans.setData(SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes());

        lagre(prosessinstans);
    }

    Prosessinstans lagJournalføringProsessinstans(ProsessType type, JournalfoeringRequest journalfoeringRequest, String institusjonID) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(type);

        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringRequest.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalfoeringRequest.getHoveddokument().getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalfoeringRequest.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringRequest.getBrukerID());
        prosessinstans.setData(ProsessDataKey.VIRKSOMHET_ORGNR, journalfoeringRequest.getVirksomhetOrgnr());

        prosessinstans.setData(ProsessDataKey.AVSENDER_TYPE, journalfoeringRequest.getAvsenderType());
        if (journalfoeringRequest.getAvsenderType() == Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET) {
            prosessinstans.setData(ProsessDataKey.AVSENDER_ID, institusjonID);
            prosessinstans.setData(ProsessDataKey.AVSENDER_LAND, journalfoeringRequest.getAvsenderID());
        } else {
            prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalfoeringRequest.getAvsenderID());
        }
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalfoeringRequest.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringRequest.getHoveddokument().getTittel());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, journalfoeringRequest.getSkalTilordnes());
        prosessinstans.setData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, journalfoeringRequest.getForvaltningsmeldingMottaker());

        if (journalfoeringRequest.getMottattDato() != null) {
            prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, journalfoeringRequest.getMottattDato());
        }

        if (!CollectionUtils.isEmpty(journalfoeringRequest.getHoveddokument().getLogiskeVedlegg())) {
            prosessinstans.setData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, journalfoeringRequest.getHoveddokument().getLogiskeVedlegg());
        }

        if (!CollectionUtils.isEmpty(journalfoeringRequest.getVedlegg())) {
            prosessinstans.setData(ProsessDataKey.FYSISKE_VEDLEGG,
                journalfoeringRequest.getVedlegg().stream().collect(Collectors.toMap(DokumentRequest::getDokumentID, DokumentRequest::getTittel)));
        }

        return prosessinstans;
    }

    @Transactional
    public void opprettManglendeInnbetalingProsessflyt(ManglendeFakturabetalingMelding manglendeFakturabetalingMelding) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING);

        prosessinstans.setData(MOTTATT_DATO, manglendeFakturabetalingMelding.getDatoMottatt());
        prosessinstans.setData(FAKTURASERIE_REFERANSE, manglendeFakturabetalingMelding.getFakturaserieReferanse());
        prosessinstans.setData(BETALINGSSTATUS, manglendeFakturabetalingMelding.getBetalingsstatus());
        prosessinstans.setData(FAKTURANUMMER, manglendeFakturabetalingMelding.getFakturanummer());
        prosessinstans.setLåsReferanse(LåsReferanseFactory.lagString(manglendeFakturabetalingMelding));

        lagre(prosessinstans);
    }

    public void opprettProsessinstansJournalføringKnyttTilEksisterende(JournalfoeringTilordneRequest journalfoeringRequest, String saksnummer, Fagsak fagsak, String institusjonID) {
        Prosessinstans prosessinstans = lagJournalføringProsessinstans(ProsessType.JFR_KNYTT, journalfoeringRequest, institusjonID);
        prosessinstans.setBehandling(fagsak.hentSistAktivBehandling());
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, journalfoeringRequest.getIngenVurdering());

        lagre(prosessinstans);
    }

    public void journalførOgOpprettAndregangsBehandling(ProsessType prosessTypeForAndregangsbehandling, Behandlingstema behandlingstema, Behandlingstyper behandlingstype,
                                                        JournalfoeringTilordneRequest journalfoeringRequest, Behandlingsaarsaktyper behandlingsaarsaktyper, LocalDate mottaksdato, String institusjonID) {

        Prosessinstans prosessinstans = lagJournalføringProsessinstans(prosessTypeForAndregangsbehandling, journalfoeringRequest, institusjonID);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, behandlingsaarsaktyper);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, mottaksdato);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, journalfoeringRequest.getSaksnummer());

        lagre(prosessinstans);
    }

    public void opprettProsessinstansJournalføringNySak(JournalfoeringOpprettRequest journalfoeringRequest, ProsessType prosessType,
                                                        boolean skalSetteSøknadslandOgPeriode, LocalDate mottaksdato, Behandlingsaarsaktyper behandlingsaarsaktyper, String institusjonID) {

        Prosessinstans prosessinstans = lagJournalføringProsessinstans(prosessType, journalfoeringRequest, institusjonID);
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, Sakstyper.valueOf(journalfoeringRequest.getFagsak().getSakstype()));
        prosessinstans.setData(ProsessDataKey.SAKSTEMA, Sakstemaer.valueOf(journalfoeringRequest.getFagsak().getSakstema()));
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.valueOf(journalfoeringRequest.getBehandlingstypeKode()));
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, behandlingsaarsaktyper);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, mottaksdato);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.valueOf(journalfoeringRequest.getBehandlingstemaKode()));

        if (skalSetteSøknadslandOgPeriode) {
            prosessinstans.setData(ProsessDataKey.SØKNADSLAND, journalfoeringRequest.getFagsak().getLand());
            prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, journalfoeringRequest.getFagsak().getSoknadsperiode());
        }

        lagre(prosessinstans);
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

    public boolean harAktivProsessinstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndStatusIs(
            behandlingID, ProsessStatus.KLAR
        ).isPresent();
    }

    public boolean harVedtakInstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndTypeIn(behandlingID,
            ProsessType.IVERKSETT_VEDTAK_FTRL,
            ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE,
            ProsessType.IVERKSETT_VEDTAK_EOS).isPresent();
    }

    public void lagre(Prosessinstans prosessinstans) { // TODO: må være private
        lagre(prosessinstans, getSaksbehandlerIdent(), getSaksbehandlerNavn());
    }

    void lagre(Prosessinstans prosessinstans, String saksbehandler, String saksbehandlerNavn) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstans.setStatus(ProsessStatus.KLAR);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER_NAVN, saksbehandlerNavn);
            logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler,
                prosessinstans.getId(), prosessinstans.getType());
        } else {
            logger.info("Melosys har opprettet prosessinstans {} av type {}.", prosessinstans.getId(),
                prosessinstans.getType());
        }
        prosessinstans.setData(CORRELATION_ID_SAKSFLYT, MDCOperations.getCorrelationId());

        prosessinstansRepo.save(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));
        prosessinstanserOpprettet.increment();
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

    public void opprettProsessinstansAnmodningOmUnntakMottakSvar(Behandling behandling, String ytterligereInfo) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR)
            .medBehandling(behandling)
            .medYtterligereinformasjonSed(ytterligereInfo)
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

    public void opprettProsessinstansIverksettVedtakFTRL(Behandling behandling, VedtakRequest request) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medBehandling(behandling)
            .build();

        prosessinstans.setData(BETALINGSINTERVALL, request.getBetalingsintervall());

        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtakTrygdeavtale(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK_TRYGDEAVTALE)
            .medBehandling(behandling)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySakEØS(String journalpostID, OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = new Prosessinstans();

        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK_EOS_FRA_OPPGAVE);
        prosessinstans.setData(VIRKSOMHET_ORGNR, opprettSakRequest.getVirksomhetOrgnr());
        prosessinstans.setData(SAKSTEMA, opprettSakRequest.getSakstema());
        prosessinstans.setData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType());
        prosessinstans.setData(MOTTATT_DATO, opprettSakRequest.getMottaksdato());
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, opprettSakRequest.getSakstype());
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, opprettSakRequest.getBrukerID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, opprettSakRequest.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes());
        if (opprettSakRequest.getSoknad().getPeriode() != null) {
            prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, opprettSakRequest.getSoknad().getPeriode());
        }
        if (opprettSakRequest.getSoknad().getLand() != null) {
            prosessinstans.setData(ProsessDataKey.SØKNADSLAND, opprettSakRequest.getSoknad().getLand());
        }

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySakFTRLTrygdeavtale(String journalpostID, OpprettSakRequest opprettSakRequest) {
        Prosessinstans prosessinstans = new Prosessinstans();

        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK_FTRL_TRYGDEAVTALE_FRA_OPPGAVE);
        prosessinstans.setData(VIRKSOMHET_ORGNR, opprettSakRequest.getVirksomhetOrgnr());
        prosessinstans.setData(BEHANDLINGSÅRSAKTYPE, opprettSakRequest.getBehandlingsaarsakType());
        prosessinstans.setData(MOTTATT_DATO, opprettSakRequest.getMottaksdato());
        prosessinstans.setData(ProsessDataKey.SAKSTYPE, opprettSakRequest.getSakstype());
        prosessinstans.setData(ProsessDataKey.SAKSTEMA, opprettSakRequest.getSakstema());
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, opprettSakRequest.getBehandlingstype());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, opprettSakRequest.getBehandlingstema());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, opprettSakRequest.getBrukerID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, opprettSakRequest.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, opprettSakRequest.getSkalTilordnes());

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

        prosessinstans.setData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VEDTAK);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansRegistrerUnntakFraMedlemskap(Behandling behandling, Saksstatuser saksstatus) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.REGISTRERE_UNNTAK_FRA_MEDLEMSKAP)
            .build();

        prosessinstans.setData(SAKSSTATUS, saksstatus);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettIkkeYrkesaktiv(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV)
            .build();

        prosessinstans.setData(SAKSSTATUS, Saksstatuser.LOVVALG_AVKLART);

        lagre(prosessinstans);
    }

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

    public void opprettProsessinstansUnntaksperiodeAvvist(Behandling behandling, String begrunnelseFritekst) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK_AVVIS)
            .medBehandling(behandling)
            .medBegrunnelseFritekst(begrunnelseFritekst)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansOpprettOgDistribuerBrev(Behandling behandling, Mottaker mottaker, DokgenBrevbestilling brevbestilling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_OG_DISTRIBUER_BREV);
        prosessinstans.setData(BREVBESTILLING, brevbestilling);
        prosessinstans.setData(MOTTAKER, mottaker.getRolle());
        if (hasText(mottaker.getAktørId())) {
            prosessinstans.setData(AKTØR_ID, mottaker.getAktørId());
        }
        if (hasText(mottaker.getPersonIdent())) {
            prosessinstans.setData(PERSON_IDENT, mottaker.getPersonIdent());
        }
        if (hasText(mottaker.getOrgnr())) {
            prosessinstans.setData(ORGNR, mottaker.getOrgnr());
        }
        if (hasText(mottaker.getInstitusjonID())) {
            // TODO Parsing av variabelen feiler pga ":". Burde fikses på en skikkelig måte
            prosessinstans.setData(INSTITUSJON_ID, String.format("\"%s\"", mottaker.getInstitusjonID()));
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

        prosessinstans.setData(ProsessDataKey.DISTRIBUSJONSTYPE, Distribusjonstype.VIKTIG);

        lagre(prosessinstans);
    }

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
        prosessinstans.setData(SAKSTEMA, Sakstemaer.UNNTAK);
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

        prosessinstans.setData(SAKSTEMA, Sakstemaer.UNNTAK);
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

    public void opprettProsessinstanserSendBrev(Behandling behandling, DoksysBrevbestilling brevbestilling, List<Mottaker> mottakere) {
        for (Mottaker mottaker : mottakere) {
            opprettProsessinstansSendBrev(behandling, brevbestilling, mottaker);
        }
    }

    public void opprettProsessinstansSendBrev(Behandling behandling, DoksysBrevbestilling brevbestilling, Mottaker mottaker) {
        brevbestilling.settMottaker(mottaker);

        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.SEND_BREV)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(BREVBESTILLING, brevbestilling);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansOppdaterFaktura(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.OPPDATER_FAKTURA)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(SAKSNUMMER, behandling.getFagsak().getSaksnummer());

        lagre(prosessinstans);
    }
}
