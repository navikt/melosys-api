package no.nav.melosys.service.avklartefakta;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
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

    private final String valgtFakta = "TRUE";

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

    public Avklartefakta hentAvklarteFakta(long behandlingsresultatID, Avklartefaktatype type) throws FunksjonellException {
        return avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsresultatID, type)
            .orElseThrow(() -> new FunksjonellException("Avklartefakta " + type + " mangler for behandlingsresultat " + behandlingsresultatID));
    }

    public Set<Avklartefakta> hentAlleAvklarteFlaggland(long behandlingsid) {
        return avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatype.FLAGGLAND);
    }

    public Optional<Landkoder> hentFlaggland(long behandlingsid) {
        return hentAlleAvklarteFlaggland(behandlingsid).stream()
            .map(af -> Landkoder.valueOf(af.getFakta()))
            .findFirst();
    }

    public Optional<Landkoder> hentBostedland(long behandlingsid) {
        return avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatype.BOSTEDSLAND).stream()
            .map(af -> Landkoder.valueOf(af.getFakta()))
            .findFirst();
    }

    public Set<String> hentAvklarteOrganisasjoner(long behandlingsid) {
        Set<Avklartefakta> avklartefakta =
                avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingsid,
                                                                                   Avklartefaktatype.AVKLARTE_ARBEIDSGIVER,
                                                                                   valgtFakta);
        return avklartefakta.stream()
                .map(Avklartefakta::getSubjekt)
                .collect(Collectors.toSet());
    }

    public Yrkesgrupper hentYrkesGruppe(long behandlingsid) throws TekniskException {
        Optional<Avklartefakta> avklartefaktaOpt =
                avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatype.YRKESGRUPPE);

        Avklartefakta avklartefakta = avklartefaktaOpt.orElseThrow(() -> new TekniskException("Finner ingen avklartefakta for YRKESGRUPPE"));
        AvklartYrkesgruppeType aktivitetType = AvklartYrkesgruppeType.valueOf(avklartefakta.getFakta());

        return aktivitetType.tilYrkesgruppeType();
    }

    public Optional<Maritimtyper> hentMaritimType(long behandlingsid) {
        Optional<Avklartefakta> avklartefaktaOpt =
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatype.SOKKEL_ELLER_SKIP);
        return avklartefaktaOpt.map(af -> Maritimtyper.valueOf(af.getFakta()));
    }

    @Transactional
    public void lagreAvklarteFakta(long behandlingsid, Set<AvklartefaktaDto> avklartefaktaDtos) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke behandlingsresultat for behandlingsid: " + behandlingsid));

        avklarteFaktaRepository.deleteByBehandlingsresultat(resultat);

        List<Avklartefakta> avklartefaktaList = avklartefaktaDtos.stream()
            .map(avklartefaktaDto -> faktaKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, resultat))
            .collect(Collectors.toList());

        avklarteFaktaRepository.saveAll(avklartefaktaList);
    }

    @Transactional
    public void leggTilÅrsakEndringPeriode(long behandlingsid, Endretperioder endretperiode) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke behandlingsresultat for behandlingsid: " + behandlingsid));

        Set<Avklartefakta> avklartefaktaSet = avklarteFaktaRepository.findByBehandlingsresultatId(behandlingsid);
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setBehandlingsresultat(resultat);
        avklartefakta.setType(Avklartefaktatype.AARSAK_ENDRING_PERIODE);
        avklartefakta.setFakta(endretperiode.getKode());
        avklartefakta.setReferanse(Avklartefaktatype.AARSAK_ENDRING_PERIODE.getKode());
        avklartefaktaSet.add(avklartefakta);
        avklarteFaktaRepository.saveAll(avklartefaktaSet);
    }
}