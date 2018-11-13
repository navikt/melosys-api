package no.nav.melosys.service.avklartefakta;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.domain.AvklartefaktaType;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingResultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvklartefaktaService {

    private final AvklarteFaktaRepository avklarteFaktaRepository;

    private final BehandlingResultatRepository behandlingResultatRepository;

    private final AvklartefaktaDtoKonverterer faktaKonverterer;

    @Autowired
    public AvklartefaktaService(AvklarteFaktaRepository avklarteFaktaRepository, BehandlingResultatRepository behandlingResultatRepository, AvklartefaktaDtoKonverterer faktaKonverterer) {
        this.avklarteFaktaRepository = avklarteFaktaRepository;
        this.behandlingResultatRepository = behandlingResultatRepository;
        this.faktaKonverterer = faktaKonverterer;
    }

    // Ment som eksempel på å hente ut avklarte fakta. Ikke i bruk enda
    // FIXME skal dette brukes?
    public Landkoder hentBosted(long behandlingsid) throws IkkeFunnetException {
        Optional<Avklartefakta> fakta = avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, AvklartefaktaType.BOSTEDSLAND);
        if (!fakta.isPresent()) {
            throw new IkkeFunnetException("Fant ikke avklartefakta for behandlingsid: " + behandlingsid);
        }

        return Landkoder.valueOf(fakta.get().getFakta());
    }

    @Transactional
    public Set<AvklartefaktaDto> hentAvklarteFakta(long behandlingsid) {
        Set<Avklartefakta> avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatId(behandlingsid);

        return avklartefakta.stream().map(AvklartefaktaDto::new).collect(Collectors.toSet());
    }

    @Transactional
    public void lagreAvklarteFakta(long behandlingsid, Set<AvklartefaktaDto> avklartefaktaDtos) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingResultatRepository.findOne(behandlingsid);
        if (resultat == null) {
            throw new IkkeFunnetException("Fant ikke behandlingsresultat for behandlingsid: " + behandlingsid);
        }

        for (AvklartefaktaDto avklarteFaktaDto : avklartefaktaDtos) {
            Optional<Avklartefakta> resultOpt =
                    avklarteFaktaRepository.findByBehandlingsresultatAndReferanseAndSubjekt(resultat,
                                                                                            avklarteFaktaDto.getReferanse(),
                                                                                            avklarteFaktaDto.getSubjektID());

            resultOpt.ifPresent(avklartefakta -> {
                avklarteFaktaRepository.delete(avklartefakta.getId());
            });

            Avklartefakta avklartefakta = new Avklartefakta();
            avklartefakta.setBehandlingsresultat(resultat);

            faktaKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklarteFaktaDto);
            avklarteFaktaRepository.save(avklartefakta);
        }
    }
}