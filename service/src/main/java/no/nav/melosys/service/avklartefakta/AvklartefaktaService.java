package no.nav.melosys.service.avklartefakta;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.service.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvklartefaktaService {

    private final AvklarteFaktaRepository avklarteFaktaRepository;

    private final BehandlingService behandlingService;

    private final AvklartefaktaDtoKonverterer faktaKonverterer;

    @Autowired
    public AvklartefaktaService(AvklarteFaktaRepository avklarteFaktaRepository, BehandlingService behandlingService, AvklartefaktaDtoKonverterer faktaKonverterer) {
        this.avklarteFaktaRepository = avklarteFaktaRepository;
        this.behandlingService = behandlingService;
        this.faktaKonverterer = faktaKonverterer;
    }

    public Landkoder hentBosted(long behandlingsid) throws IkkeFunnetException {
        Optional<Avklartefakta> fakta = avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, AvklartefaktaType.BOSTEDSLAND);
        if (!fakta.isPresent()) {
            throw new IkkeFunnetException("Fant ikke avklartefakta for behandlingsid: "+ behandlingsid);
        }

        return Landkoder.valueOf(fakta.get().getFakta());
    }

    @Transactional
    public void lagreAvklarteFakta(long behandlingsid, Set<AvklartefaktaDto> avklartefaktaDtos) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingService.hentBehandlingresultat(behandlingsid);
        if (resultat == null) {
            throw new IkkeFunnetException("Fant ikke behandlingsresultat for behandlingsid: "+ behandlingsid);
        }

        for (AvklartefaktaDto avklarteFaktaDto : avklartefaktaDtos) {
            Optional<Avklartefakta> resultOpt = avklarteFaktaRepository.findByBehandlingsresultatAndReferanse(resultat, avklarteFaktaDto.getReferanse());
            Avklartefakta avklartefakta = resultOpt.orElseGet(() -> new Avklartefakta());
            avklartefakta.setBehandlingsresultat(resultat);

            faktaKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklarteFaktaDto);
            avklarteFaktaRepository.save(avklartefakta);
        }
    }

    @Transactional
    public Set<AvklartefaktaDto> hentAvklarteFakta(long behandlingsid) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingService.hentBehandlingresultat(behandlingsid);
        if (resultat == null) {
            throw new IkkeFunnetException("Fant ikke behandlingsresultat for behandlingsid: "+ behandlingsid);
        }

        Set<Avklartefakta> avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatId(behandlingsid);
        return faktaKonverterer.lagDtoFraAvklartefakta(avklartefakta);
    }
}