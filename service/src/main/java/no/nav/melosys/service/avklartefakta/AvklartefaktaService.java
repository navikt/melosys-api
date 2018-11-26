package no.nav.melosys.service.avklartefakta;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
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

    @Transactional
    public Set<AvklartefaktaDto> hentAlleAvklarteFakta(long behandlingsid) {
        Set<Avklartefakta> avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatId(behandlingsid);

        return avklartefakta.stream().map(AvklartefaktaDto::new).collect(Collectors.toSet());
    }

    public Set<String> hentAvklarteOrganisasjoner(long behandlingsid) throws IkkeFunnetException {
        Set<Avklartefakta> avklartefakta =
                avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingsid,
                                                                                   AvklartefaktaType.AVKLARTE_ARBEIDSGIVER,
                                                                            "TRUE");
        return avklartefakta.stream()
                .map(Avklartefakta::getSubjekt)
                .collect(Collectors.toSet());
    }

    public YrkesgruppeType hentYrkesGruppe(long behandlingsid) throws TekniskException {
        Optional<Avklartefakta> avklartefaktaOpt =
                avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, AvklartefaktaType.YRKESGRUPPE);

        Avklartefakta avklartefakta = avklartefaktaOpt.orElseThrow(() -> new TekniskException("Finner ingen avklartefakta for yrkesgruppe"));
        AvklartYrkesgruppeType aktivitetType = AvklartYrkesgruppeType.valueOf(avklartefakta.getFakta());

        return aktivitetType.tilYrkesgruppeType();
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
            Avklartefakta avklartefakta = resultOpt.orElseGet(Avklartefakta::new);
            avklartefakta.setBehandlingsresultat(resultat);

            faktaKonverterer.oppdaterAvklartefaktaFraDto(avklartefakta, avklarteFaktaDto);
            avklarteFaktaRepository.save(avklartefakta);
        }
    }
}