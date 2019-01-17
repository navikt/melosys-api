package no.nav.melosys.service;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProsessinstansService {
    private final Binge binge;
    private final ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public ProsessinstansService(Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    public void opprettProsessinstansIverksettVedtak(Behandling behandling, BehandlingsresultatType behandlingsresultatType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
        prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, behandlingsresultatType.getKode());
        prosessinstans.setBehandling(behandling);

        lagreProsessinstans(prosessinstans);
    }

    public void opprettProsessinstansAnmodningOmUnntak(Behandling behandling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.AOU_VALIDERING);
        prosessinstans.setBehandling(behandling);

        lagreProsessinstans(prosessinstans);
    }

    public void lagreProsessinstans(Prosessinstans prosessinstans) {
        lagreProsessinstans(prosessinstans, SubjectHandler.getInstance().getUserID());
    }

    public void lagreProsessinstans(Prosessinstans prosessinstans, String saksbehandler) {
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        if (saksbehandler != null) {
            prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
        }

        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
