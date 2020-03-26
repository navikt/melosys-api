package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
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

    private static final EnumSet<Avklartefaktatyper> maritimeFaktatyper = EnumSet.of(
        Avklartefaktatyper.SOKKEL_ELLER_SKIP,
        Avklartefaktatyper.ARBEIDSLAND);

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

    public Set<Landkoder> hentAlleAvklarteArbeidsland(long behandlingsid) {
        Set<Avklartefakta> avklarteArbeidsland =
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatyper.ARBEIDSLAND);

        return avklarteArbeidsland.stream()
            .map(af -> Landkoder.valueOf(af.getSubjekt()))
            .collect(Collectors.toSet());
    }

    public Optional<Landkoder> hentBostedland(long behandlingsid) {
        return avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatyper.BOSTEDSLAND).stream()
            .map(af -> Landkoder.valueOf(af.getFakta()))
            .findFirst();
    }

    // Denne leverer enten norske orgnr eller uuid for identifikasjon av utenlandske foretak
    public Set<String> hentAvklarteOrgnrOgUuid(long behandlingsid) {
        Set<Avklartefakta> avklartefakta =
                avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingsid, Avklartefaktatyper.VIRKSOMHET, VALGT_FAKTA);
        return avklartefakta.stream()
                .map(Avklartefakta::getSubjekt)
                .collect(Collectors.toSet());
    }

    public Optional<Yrkesgrupper> finnYrkesGruppe(long behandlingsid) throws TekniskException {
        final var avklartYrkesgruppeType = avklarteFaktaRepository
            .findByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatyper.YRKESGRUPPE)
            .map(Avklartefakta::getFakta).map(AvklartYrkesgruppeType::valueOf).orElse(null);

        return (avklartYrkesgruppeType == null) ? Optional.empty() : Optional.of(avklartYrkesgruppeType.tilYrkesgruppeType());
    }

    public Set<Landkoder> hentLandkoderMedMarginaltArbeid(long behandlingsid) {
        Collection<Avklartefakta> marginaltArbeid =
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingsid, Avklartefaktatyper.MARGINALT_ARBEID, VALGT_FAKTA);

        return marginaltArbeid.stream()
            .map(Avklartefakta::getSubjekt)
            .map(Landkoder::valueOf)
            .collect(Collectors.toSet());
    }

    public boolean harMarginaltArbeid(long behandlingsid) {
        Collection<Avklartefakta> marginaltArbeid =
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingsid, Avklartefaktatyper.MARGINALT_ARBEID, VALGT_FAKTA);
        return !marginaltArbeid.isEmpty();
    }

    public Optional<Maritimtyper> hentMaritimType(long behandlingsid) {
        Optional<Avklartefakta> avklartefaktaOpt =
            avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, Avklartefaktatyper.SOKKEL_ELLER_SKIP);
        return avklartefaktaOpt.map(af -> Maritimtyper.valueOf(af.getFakta()));
    }

    public Map<String, AvklartMaritimtArbeid> hentAlleMaritimeAvklartfakta(long behandlingsid) {
        Set<Avklartefakta> maritimeAvklartefakta =
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(behandlingsid, maritimeFaktatyper);

        Map<String, List<Avklartefakta>> maritimtArbeidGruppert = grupperForSubjekt(maritimeAvklartefakta);
        return maritimtArbeidGruppert.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, AvklartMaritimtArbeid::av));
    }

    private Map<String, List<Avklartefakta>> grupperForSubjekt(Collection<Avklartefakta> avklartefakta) {
        return avklartefakta.stream()
            .collect(Collectors.groupingBy(Avklartefakta::getSubjekt, Collectors.toList()));
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagreAvklarteFakta(long behandlingsid, Set<AvklartefaktaDto> avklartefaktaDtos) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(FANT_IKKE_RESULTAT + behandlingsid));

        avklarteFaktaRepository.deleteByBehandlingsresultatId(behandlingsid);
        avklarteFaktaRepository.flush();

        List<Avklartefakta> avklartefaktaList = avklartefaktaDtos.stream()
            .map(avklartefaktaDto -> faktaKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, resultat))
            .collect(Collectors.toList());

        avklarteFaktaRepository.saveAll(avklartefaktaList);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilAvklarteFakta(long behandlingsid, Avklartefaktatyper type, String referanse, String subjekt, String fakta) throws IkkeFunnetException {
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
    public void leggTilBegrunnelse(long behandlingsid, Avklartefaktatyper avklartefaktatype, String begrunnelseKode) throws IkkeFunnetException {
        leggTilAvklarteFakta(behandlingsid, avklartefaktatype, avklartefaktatype.getKode(), null, begrunnelseKode);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilRegistrering(long behandlingsid, Avklartefaktatyper avklartefaktatype, String begrunnelseKode) throws IkkeFunnetException {
        Avklartefakta avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingsid, avklartefaktatype)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke avklarte fakta av type " + avklartefaktatype.name() + "for behandling " + behandlingsid));

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelseKode);
        registrering.setAvklartefakta(avklartefakta);

        avklartefakta.getRegistreringer().add(registrering);

        avklarteFaktaRepository.save(avklartefakta);
    }
}