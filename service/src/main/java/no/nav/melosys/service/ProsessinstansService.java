package no.nav.melosys.service;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.journalforing.dto.JournalfoeringDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.ProsessSteg.HS_OPPDATER_RESULTAT;

@Service
public class ProsessinstansService {

    private static Logger logger = LoggerFactory.getLogger(ProsessinstansService.class);

    private final Binge binge;
    private final ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public ProsessinstansService(Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
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

    public void lagreOgSettIBingen(Prosessinstans prosessinstans) {
        lagreOgSettIBingen(prosessinstans, SubjectHandler.getInstance().getUserID());
    }

    void lagreOgSettIBingen(Prosessinstans prosessinstans, String saksbehandler) {

        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        }

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);

        logger.info("Saksbehandler={} har opprettet prosessinstans {} av type {}.", saksbehandler, prosessinstans.getId(), prosessinstans.getType());
    }

    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.AOU_VALIDERING);
        prosessinstans.setBehandling(behandling);

        lagreOgSettIBingen(prosessinstans);
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

        lagreOgSettIBingen(prosessinstans);
    }

    public void opprettProsessinstansIverksettVedtak(Behandling behandling, Behandlingsresultattyper behandlingsresultatType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        prosessinstans.setBehandling(behandling);

        lagreOgSettIBingen(prosessinstans);
    }
}
