package no.nav.melosys.service.saksflyt;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.HS_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Service
public class ProsessinstansService {

    private static Logger logger = LoggerFactory.getLogger(ProsessinstansService.class);

    private final ProsessinstansRepository prosessinstansRepo;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public ProsessinstansService(ProsessinstansRepository prosessinstansRepo, ApplicationEventPublisher applicationEventPublisher) {
        this.prosessinstansRepo = prosessinstansRepo;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public static Prosessinstans lagJournalføringProsessinstans(ProsessType type, JournalfoeringDto journalfoeringDto) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(type);
        prosessinstans.setSteg(ProsessSteg.JFR_VALIDERING);

        if (!StringUtils.isEmpty(journalfoeringDto.getBehandlingstypeKode())) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.valueOf(journalfoeringDto.getBehandlingstypeKode()));
        }
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalfoeringDto.getJournalpostID());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, journalfoeringDto.getDokumentID());
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, journalfoeringDto.getOppgaveID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, journalfoeringDto.getBrukerID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, journalfoeringDto.getAvsenderID());
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, journalfoeringDto.getAvsenderNavn());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, journalfoeringDto.getHoveddokumentTittel());
        return prosessinstans;
    }

    public boolean erUnderOppfriskning(Long behandlingID) {
        return prosessinstansRepo.findByTypeAndBehandling_IdAndStegIsNotAndStegIsNot(ProsessType.OPPFRISKNING, behandlingID, ProsessSteg.FEILET_MASKINELT, ProsessSteg.FERDIG).isPresent();
    }

    public boolean harAktivProsessinstans(Long behandlingID) {
        return prosessinstansRepo.findByBehandling_IdAndStegIsNotAndStegIsNot(behandlingID, ProsessSteg.FEILET_MASKINELT, ProsessSteg.FERDIG).isPresent();
    }

    public void lagre(Prosessinstans prosessinstans) {
        lagre(prosessinstans, SubjectHandler.getInstance().getUserID());
    }

    @Transactional
    void lagre(Prosessinstans prosessinstans, String saksbehandler) {

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        }

        prosessinstansRepo.save(prosessinstans);
        applicationEventPublisher.publishEvent(new ProsessinstansOpprettetEvent(prosessinstans));

        logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler, prosessinstans.getId(), prosessinstans.getType());
    }

    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.AOU_VALIDERING);
        prosessinstans.setBehandling(behandling);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansHenleggSak(Behandling behandling, Henleggelsesgrunner begrunnelseKode, String fritekst) {
        Prosessinstans prosessinstans = new Prosessinstans();

        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());
        prosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, begrunnelseKode);
        if (begrunnelseKode == Henleggelsesgrunner.ANNET) {
            prosessinstans.setData(ProsessDataKey.FRITEKST, fritekst);
        }

        prosessinstans.setSteg(HS_OPPDATER_RESULTAT);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultatType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        prosessinstans.setBehandling(behandling);

        lagre(prosessinstans);
    }

    public void opprettProsessinstansOppfriskning(Behandling behandling, String aktørID, String brukerID, SoeknadDokument søknadDokument) {
        Prosessinstans nyprosessinstans = new Prosessinstans();
        nyprosessinstans.setBehandling(behandling);
        nyprosessinstans.setType(ProsessType.OPPFRISKNING);

        nyprosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        nyprosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);

        nyprosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, hentPeriode(søknadDokument));
        nyprosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, hentLand(søknadDokument));

        nyprosessinstans.setSteg(ProsessSteg.JFR_HENT_PERS_OPPL);

        LocalDateTime nå = LocalDateTime.now();
        nyprosessinstans.setRegistrertDato(nå);
        nyprosessinstans.setEndretDato(nå);

        lagre(nyprosessinstans);
    }

    public void opprettProsessinstansForkortPeriode(Behandling behandling, Endretperioder endretperiode) {
        Prosessinstans nyprosessinstans = new Prosessinstans();

        String saksbehandler = SubjectHandler.getInstance().getUserID();
        nyprosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        nyprosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, endretperiode);
        nyprosessinstans.setBehandling(behandling);
        nyprosessinstans.setType(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);
        nyprosessinstans.setSteg(ProsessSteg.IV_FORKORT_PERIODE);

        LocalDateTime nå = LocalDateTime.now();
        nyprosessinstans.setRegistrertDato(nå);
        nyprosessinstans.setEndretDato(nå);

        lagre(nyprosessinstans);
    }
}
