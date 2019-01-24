package no.nav.melosys.service.avklartefakta;

import java.util.List;
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
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvklartefaktaService {

    private final AvklarteFaktaRepository avklarteFaktaRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final AvklartefaktaDtoKonverterer faktaKonverterer;

    @Autowired
    public AvklartefaktaService(AvklarteFaktaRepository avklarteFaktaRepository, BehandlingsresultatRepository behandlingsresultatRepository, AvklartefaktaDtoKonverterer faktaKonverterer) {
        this.avklarteFaktaRepository = avklarteFaktaRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.faktaKonverterer = faktaKonverterer;
    }

    @Transactional
    public Set<AvklartefaktaDto> hentAlleAvklarteFakta(long behandlingsid) {
        Set<Avklartefakta> avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatId(behandlingsid);

        return avklartefakta.stream().map(AvklartefaktaDto::new).collect(Collectors.toSet());
    }

    public Set<Avklartefakta> hentAlleAvklarteArbeidsland(long behandlingsid) {
        return avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingsid, AvklartefaktaType.BOSTEDSLAND);
    }

    public Set<String> hentAvklarteOrganisasjoner(long behandlingsid) {
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
        Behandlingsresultat resultat = behandlingsresultatRepository.findOne(behandlingsid);
        if (resultat == null) {
            throw new IkkeFunnetException("Fant ikke behandlingsresultat for behandlingsid: " + behandlingsid);
        }

        avklarteFaktaRepository.deleteByBehandlingsresultat(resultat);

        List<Avklartefakta> avklartefaktaList = avklartefaktaDtos.stream()
            .map(avklartefaktaDto -> faktaKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, resultat))
            .collect(Collectors.toList());

        avklarteFaktaRepository.save(avklartefaktaList);
    }
}