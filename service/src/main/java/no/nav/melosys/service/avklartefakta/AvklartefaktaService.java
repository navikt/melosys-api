package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
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

    private static final String VALGT_FAKTA = "TRUE";
    private static final String FANT_IKKE_RESULTAT = "Fant ikke behandlingsresultat for behandlingsid: ";

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
        return avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatype.ARBEIDSLAND);
    }

    public Optional<Landkoder> hentArbeidsland(long behandlingsid) {
        return hentAlleAvklarteArbeidsland(behandlingsid).stream()
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
                avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingsid, Avklartefaktatype.VIRKSOMHET, VALGT_FAKTA);
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

    public Collection<AvklartMaritimtArbeid> hentMaritimeAvklartfakta(long behandlingsid) {
        Collection<Avklartefaktatype> maritimeFaktatyper = Arrays.asList(
            Avklartefaktatype.SOKKEL_ELLER_SKIP,
            Avklartefaktatype.ARBEIDSLAND);

        Set<Avklartefakta> maritimeAvklartefakta =
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(behandlingsid, maritimeFaktatyper);

        HashMap<String, AvklartMaritimtArbeid> maritimeFaktaGruppert = new HashMap<>();
        for (Avklartefakta avklartefakta : maritimeAvklartefakta) {
            String navn = avklartefakta.getSubjekt();
            if (!maritimeFaktaGruppert.containsKey(navn)) {
                maritimeFaktaGruppert.put(navn, new AvklartMaritimtArbeid(navn));
            }
            maritimeFaktaGruppert.get(navn).leggTilFakta(avklartefakta);
        }

        return maritimeFaktaGruppert.values();
    }

    public Optional<Avklartefakta> hentVurderingUnntakPeriode(long behandlingsid) {
        return avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatype.VURDERING_UNNTAK_PERIODE);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagreAvklarteFakta(long behandlingsid, Set<AvklartefaktaDto> avklartefaktaDtos) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(FANT_IKKE_RESULTAT + behandlingsid));

        avklarteFaktaRepository.deleteByBehandlingsresultat(resultat);

        List<Avklartefakta> avklartefaktaList = avklartefaktaDtos.stream()
            .map(avklartefaktaDto -> faktaKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, resultat))
            .collect(Collectors.toList());

        avklarteFaktaRepository.saveAll(avklartefaktaList);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilAvklarteFakta(long behandlingsid, Avklartefaktatype type, String referanse, String subjekt, String fakta) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(FANT_IKKE_RESULTAT + behandlingsid));

        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(type);
        avklartefakta.setReferanse(referanse);
        avklartefakta.setSubjekt(subjekt);
        avklartefakta.setFakta(fakta);
        avklartefakta.setBehandlingsresultat(resultat);

        avklarteFaktaRepository.save(avklartefakta);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilBegrunnelse(long behandlingsid, Avklartefaktatype avklartefaktatype, String begrunnelseKode) throws IkkeFunnetException {
        leggTilAvklarteFakta(behandlingsid, avklartefaktatype, avklartefaktatype.getKode(), null, begrunnelseKode);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilRegistrering(long behandlingsid, Avklartefaktatype avklartefaktatype, String begrunnelseKode) throws IkkeFunnetException {
        Avklartefakta avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, avklartefaktatype)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke avklarte fakta av type " + avklartefaktatype.name() + "for behandling " + behandlingsid));

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelseKode);
        registrering.setAvklartefakta(avklartefakta);

        avklartefakta.getRegistreringer().add(registrering);

        avklarteFaktaRepository.save(avklartefakta);
    }
}