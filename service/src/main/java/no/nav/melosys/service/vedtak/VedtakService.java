package no.nav.melosys.service.vedtak;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VedtakService {

    private static final Logger log = LoggerFactory.getLogger(VedtakService.class);

    private final BehandlingRepository behandlingRepository;

    private final Binge binge;

    private final ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public VedtakService(BehandlingRepository behandlingRepository, Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.behandlingRepository = behandlingRepository;
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    public void fattVedtak(long behandlingID) throws IkkeFunnetException {
        log.info("Fatter vedtak for behandling: " + behandlingID);
        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Kan ikke fatte vedtak fordi behandling " + behandlingID + " finnes ikke.");
        }

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);

        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SubjectHandler.getInstance().getUserID());

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
