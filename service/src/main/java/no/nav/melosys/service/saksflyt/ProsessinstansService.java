package no.nav.melosys.service.saksflyt;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.metrics.MetrikkerNavn;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.journalforing.dto.DokumentDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.service.soknad.SoknadMottatt;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;

@Service
public class ProsessinstansService {
    private static final Logger logger = LoggerFactory.getLogger(ProsessinstansService.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProsessinstansRepository prosessinstansRepo;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    private final Counter prosessinstanserOpprettet = Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET);

    @Autowired
    public ProsessinstansService(ApplicationEventPublisher applicationEventPublisher,
                                 ProsessinstansRepository prosessinstansRepo,
                                 UtenlandskMyndighetService utenlandskMyndighetService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.prosessinstansRepo = prosessinstansRepo;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    public Prosessinstans lagJournalføringProsessinstans(ProsessType type, JournalfoeringDto journalfoeringDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(type);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);

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
            prosessinstans.setData(ProsessDataKey.FYSISKE_VEDLEGG,
                journalfoeringDto.getVedlegg().stream().collect(Collectors.toMap(DokumentDto::getDokumentID, DokumentDto::getTittel)));
        }

        return prosessinstans;
    }

    private String lagInstitusjonsId(String avsenderID) {
        try {
            return utenlandskMyndighetService.lagInstitusjonsId(Landkoder.valueOf(avsenderID));
        } catch (TekniskException e) {
            logger.warn(e.getMessage());
            logger.warn("Bruker {}: som avsenderID", avsenderID);
            return avsenderID + ":";
        }
    }

    private static boolean skalSendesForvaltningsmelding(JournalfoeringDto journalfoeringDto) {
        return journalfoeringDto.isIkkeSendForvaltingsmelding() != null && !journalfoeringDto.isIkkeSendForvaltingsmelding();
    }

    public boolean harAktivProsessinstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndStegIsNotAndStegIsNot(behandlingID, ProsessSteg.FEILET_MASKINELT, ProsessSteg.FERDIG).isPresent();
    }

    public void lagre(Prosessinstans prosessinstans) {
        lagre(prosessinstans, SubjectHandler.getInstance().getUserID());
    }

    void lagre(Prosessinstans prosessinstans, String saksbehandler) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        }

        prosessinstansRepo.save(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));
        prosessinstanserOpprettet.increment();

        logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler, prosessinstans.getId(), prosessinstans.getType());
    }

    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling, List<String> mottakerInstitusjon, String ytterligereInformasjonSed) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK)
            .medSteg(ProsessSteg.AOU_VALIDERING)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerInstitusjon)
            .medYtterligereinformasjonSed(ytterligereInformasjonSed)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansAnmodningOmUnntakMottakSvar(Behandling behandling) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK_SVAR)
            .medSteg(ProsessSteg.AOU_MOTTAK_SVAR_OPPDATER_MEDL)
            .medBehandling(behandling)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansHenleggSak(Behandling behandling, Henleggelsesgrunner begrunnelseKode, String fritekst) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
        .medBehandling(behandling)
        .medType(ProsessType.HENLEGG_SAK)
        .medSteg(ProsessSteg.HS_OPPDATER_RESULTAT)
        .medBegrunnelseFritekst(fritekst)
        .build();

        prosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, begrunnelseKode);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultatType,
                                                     String fritekst, String fritekstSed, List<String> mottakerinstitusjoner,
                                                     Vedtakstyper vedtakstype, String revurderBegrunnelse) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.IVERKSETT_VEDTAK)
            .medSteg(ProsessSteg.IV_VALIDERING)
            .medBehandling(behandling)
            .medBegrunnelseFritekst(fritekst)
            .medEessiMottakere(mottakerinstitusjoner)
            .medYtterligereinformasjonSed(fritekstSed)
            .build();

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        prosessinstans.setData(ProsessDataKey.VEDTAKSTYPE, vedtakstype.getKode());
        if (StringUtils.isNotEmpty(revurderBegrunnelse)) {
            prosessinstans.setData(ProsessDataKey.REVURDER_BEGRUNNELSE, revurderBegrunnelse);
        }

        lagre(prosessinstans);
    }

    public void opprettProsessinstansMangelbrev(Behandling behandling, Aktoersroller mottaker, BrevData brevData) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.MANGELBREV);
        prosessinstans.setSteg(ProsessSteg.MANGELBREV);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker);
        prosessinstans.setData(ProsessDataKey.BREVDATA, brevData);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansNySak(String journalpostID, OpprettSakDto opprettSakDto) throws FunksjonellException {
        if (!erBehandlingAvSøknad(opprettSakDto.getBehandlingstema().getKode())) {
            throw new FunksjonellException("Opprettelse av behandling " + opprettSakDto.getBehandlingstema()
                + " på bakgrunn av journalførte dokumenter er ikke støttet.");
        }
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.OPPRETT_NY_SAK)
            .medSteg(ProsessSteg.JFR_AKTØR_ID)
            .build();
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, opprettSakDto.getBehandlingstema());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, opprettSakDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, opprettSakDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, opprettSakDto.getSoknadDto().getPeriode());
        prosessinstans.setData(ProsessDataKey.SØKNADSLAND, opprettSakDto.getSoknadDto().getLand());
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, opprettSakDto.isSkalTilordnes());
        lagre(prosessinstans);
    }

    public void opprettProsessinstansForkortPeriode(Behandling behandling, Endretperiode endretperiode, String fritekst, String fritekstSed) {
        Prosessinstans nyprosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE)
            .medSteg(ProsessSteg.IV_FORKORT_PERIODE)
            .medBegrunnelseFritekst(fritekst)
            .medYtterligereinformasjonSed(fritekstSed)
            .build();

        nyprosessinstans.setData(ProsessDataKey.VEDTAKSTYPE, Vedtakstyper.ENDRINGSVEDTAK.getKode());
        nyprosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiode);
        lagre(nyprosessinstans);
    }

    public void opprettProsessinstansGodkjennUnntaksperiode(Behandling behandling, boolean varsleUtland) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medBehandling(behandling)
            .medType(ProsessType.REGISTRERING_UNNTAK)
            .medSteg(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL)
            .build();

        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, varsleUtland);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansUnntaksperiodeAvvist(Behandling behandling, Collection<Ikke_godkjent_begrunnelser> begrunnelser, String begrunnelseFritekst) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.REGISTRERING_UNNTAK)
            .medSteg(ProsessSteg.REG_UNNTAK_PERIODE_IKKE_GODKJENT)
            .medBehandling(behandling)
            .medBegrunnelser(begrunnelser)
            .medBegrunnelseFritekst(begrunnelseFritekst)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansForvaltningsmelding(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.FORVALTNINGSMELDING_SEND);
        prosessinstans.setSteg(ProsessSteg.SEND_FORVALTNINGSMELDING);
        prosessinstans.setBehandling(behandling);
        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansSedMottak(MelosysEessiMelding melosysEessiMelding) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_RUTING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, melosysEessiMelding.getJournalpostId());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, melosysEessiMelding.getDokumentId());
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, melosysEessiMelding.getErEndring());
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, melosysEessiMelding.getGsakSaksnummer());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, melosysEessiMelding.getAktoerId());
        lagre(prosessinstans);
    }

    public void opprettProsessinstansSedMottak(String journalpostID, String brukerID) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_HENT_EESSI_MELDING);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansGenerellSedBehandling(JournalfoeringOpprettDto journalfoeringOpprettDto) {
        Prosessinstans prosessinstans = lagJournalføringProsessinstans(ProsessType.SED_GENERELL_SAK, journalfoeringOpprettDto);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.valueOf(journalfoeringOpprettDto.getBehandlingstemaKode()));
        prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_HENT_EESSI_MELDING);
        lagre(prosessinstans);
    }

    public void opprettProsessinstansVideresendSoknad(Behandling behandling, @Nullable String mottakerInstitusjoner) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.VIDERESEND_SOKNAD)
            .medSteg(ProsessSteg.VS_OPPDATER_RESULTAT)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerInstitusjoner != null ? List.of(mottakerInstitusjoner) : null)
            .build();

        lagre(prosessinstans);
    }

    public void opprettProsessinstansUtpekAnnetLand(Behandling behandling, Landkoder utpektLand, List<String> mottakerinstitusjoner, String ytterligereInformasjonSed) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.UTPEK_LAND)
            .medSteg(ProsessSteg.UL_OPPDATER_MEDL)
            .medBehandling(behandling)
            .medEessiMottakere(mottakerinstitusjoner)
            .medYtterligereinformasjonSed(ytterligereInformasjonSed)
            .build();
        prosessinstans.setData(ProsessDataKey.UTPEKT_LAND, utpektLand);

        lagre(prosessinstans);
    }

    @Transactional
    public void opprettProsessinstansSøknadMottatt(SoknadMottatt søknadMottatt) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.MOTTAK_SOKNAD_ALTINN)
            .medSteg(ProsessSteg.MSA_HENT_INNHOLD)
            .build();
        prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, søknadMottatt.getSoknadID());

        lagre(prosessinstans);
    }

    public void opprettProsessinstansAvvisUtpeking(Behandling behandling, UtpekingAvvis utpekingAvvis) {
        Prosessinstans prosessinstans = new ProsessinstansBuilder()
            .medType(ProsessType.ARBEID_FLERE_LAND)
            .medSteg(ProsessSteg.AFL_SVAR_SEND_AVSLAG)
            .medBehandling(behandling)
            .build();
        prosessinstans.setData(ProsessDataKey.UTPEKING_AVVIS, utpekingAvvis);

        lagre(prosessinstans);
    }
}
