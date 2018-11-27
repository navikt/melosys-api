package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BehandlingService {

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public BehandlingService(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    /***
     * Metoden sjekker om en behandling med ID {@code behandlingID} har en oppfrisking i gang.
     * Oppfrisking betyr å hente saksopplysninger på nytt for en gitt behandling.
     */
    public boolean harAktivOppfrisking(long behandlingID) {
        Optional<Prosessinstans> aktivProsessinstans = prosessinstansRepository.findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessType.OPPFRISKNING, ProsessSteg.FEILET_MASKINELT, behandlingID);
        if (aktivProsessinstans.isPresent()) {
            log.debug("Behandling {} er under oppfrisking.", behandlingID);
            return true;
        }
        log.debug("Behandling {} er ikke under oppfrisking", behandlingID);
        return false;
    }

    /**
     * Oppdaterer status for en behandling med ID {@code behandlingID}.
     * Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas.
     */
    public void oppdaterStatus(long behandlingID, Behandlingsstatus status) throws FunksjonellException {
        if ((status != Behandlingsstatus.UNDER_BEHANDLING)
            && (status != Behandlingsstatus.AVVENT_DOK_PART)
            && (status != Behandlingsstatus.AVVENT_DOK_UTL)) {
            throw new FunksjonellException("Må ikke sette behanglingsstatus til " + status);
        }

        Behandling behandling = behandlingRepository.findOne(behandlingID);
        if (behandling == null) {
            throw new IkkeFunnetException("Behandling " + behandlingID + " finnes ikke.");
        }
        if (behandling.getStatus() != Behandlingsstatus.VURDER_DOKUMENT) {
            throw new FunksjonellException("Endring av status er bare mulig når behandling venter på dokumentasjon. Status var: " + behandling.getStatus());
        }
        behandling.setStatus(status);
        behandlingRepository.save(behandling);
    }
}
