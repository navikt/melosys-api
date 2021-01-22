package no.nav.melosys.service.avklartefakta;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.familie.IkkeOmfattetBarn;
import no.nav.melosys.domain.familie.OmfattetFamilie;
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

import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;

@Service
public class AvklartefaktaService {

    private final AvklarteFaktaRepository avklarteFaktaRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final AvklartefaktaDtoKonverterer faktaKonverterer;

    private static final String FANT_IKKE_RESULTAT = "Fant ikke behandlingsresultat for behandlingsid: ";

    private static final EnumSet<Avklartefaktatyper> MARITIME_FAKTATYPER = EnumSet.of(
        Avklartefaktatyper.SOKKEL_ELLER_SKIP,
        Avklartefaktatyper.ARBEIDSLAND);

    @Autowired
    public AvklartefaktaService(AvklarteFaktaRepository avklarteFaktaRepository, BehandlingsresultatRepository behandlingsresultatRepository, AvklartefaktaDtoKonverterer faktaKonverterer) {
        this.avklarteFaktaRepository = avklarteFaktaRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.faktaKonverterer = faktaKonverterer;
    }

    @Transactional
    public Set<AvklartefaktaDto> hentAlleAvklarteFakta(long behandlingID) {
        Set<Avklartefakta> avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatId(behandlingID);

        return avklartefakta.stream().map(AvklartefaktaDto::new).collect(Collectors.toSet());
    }

    public Set<Landkoder> hentAlleAvklarteArbeidsland(long behandlingID) {
        Set<Avklartefakta> avklarteArbeidsland =
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingID, Avklartefaktatyper.ARBEIDSLAND);

        return avklarteArbeidsland.stream()
            .map(af -> Landkoder.valueOf(af.getFakta()))
            .collect(Collectors.toSet());
    }

    public Optional<Landkoder> hentBostedland(long behandlingID) {
        return avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingID, Avklartefaktatyper.BOSTEDSLAND).stream()
            .map(af -> Landkoder.valueOf(af.getFakta()))
            .findFirst();
    }

    // Denne leverer enten norske orgnr eller uuid for identifikasjon av utenlandske foretak
    public Set<String> hentAvklarteOrgnrOgUuid(long behandlingID) {
        Set<Avklartefakta> avklartefakta =
                avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingID, Avklartefaktatyper.VIRKSOMHET, VALGT_FAKTA);
        return avklartefakta.stream()
                .map(Avklartefakta::getSubjekt)
                .collect(Collectors.toSet());
    }

    public Optional<Yrkesgrupper> finnYrkesGruppe(long behandlingID) throws TekniskException {
        final var avklartYrkesgruppeType = avklarteFaktaRepository
            .findByBehandlingsresultatIdAndType(behandlingID, Avklartefaktatyper.YRKESGRUPPE)
            .map(Avklartefakta::getFakta).map(AvklartYrkesgruppeType::valueOf).orElse(null);

        return (avklartYrkesgruppeType == null) ? Optional.empty() : Optional.of(avklartYrkesgruppeType.tilYrkesgruppeType());
    }

    public Set<Landkoder> hentLandkoderMedMarginaltArbeid(long behandlingID) {
        Collection<Avklartefakta> marginaltArbeid =
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingID, Avklartefaktatyper.MARGINALT_ARBEID, VALGT_FAKTA);

        return marginaltArbeid.stream()
            .map(Avklartefakta::getSubjekt)
            .map(Landkoder::valueOf)
            .collect(Collectors.toSet());
    }

    public boolean harMarginaltArbeid(long behandlingID) {
        Collection<Avklartefakta> marginaltArbeid =
            avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingID, Avklartefaktatyper.MARGINALT_ARBEID, VALGT_FAKTA);
        return !marginaltArbeid.isEmpty();
    }

    public Set<Maritimtyper> hentMaritimTyper(long behandlingID) {
        Set<Avklartefakta> avklartefakta =
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingID, Avklartefaktatyper.SOKKEL_ELLER_SKIP);
        return avklartefakta.stream()
            .map(af -> Maritimtyper.valueOf(af.getFakta()))
            .collect(Collectors.toSet());
    }

    public Optional<Landkoder> hentInformertMyndighet(long behandlingID) {
        return avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(behandlingID, Avklartefaktatyper.INFORMERT_MYNDIGHET, VALGT_FAKTA)
            .stream()
            .map(Avklartefakta::getSubjekt)
            .map(Landkoder::valueOf)
            .findFirst();
    }

    public Map<String, AvklartMaritimtArbeid> hentMaritimeAvklartfaktaEtterSubjekt(long behandlingID) {
        Set<Avklartefakta> maritimeAvklartefakta =
            avklarteFaktaRepository.findAllByBehandlingsresultatIdAndTypeIn(behandlingID, MARITIME_FAKTATYPER);

        Map<String, List<Avklartefakta>> maritimtArbeidGruppert = grupperForSubjekt(maritimeAvklartefakta);
        return maritimtArbeidGruppert.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, AvklartMaritimtArbeid::av));
    }

    public AvklarteMedfolgendeBarn hentAvklarteMedfølgendeBarn(long behandlingID) {
        Set<OmfattetFamilie> barnOmfattetAvNorskTrygd = new HashSet<>();
        Set<IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd = new HashSet<>();
        for (Avklartefakta avklartefakta : avklarteFaktaRepository.findAllByBehandlingsresultatIdAndType(behandlingID, VURDERING_LOVVALG_BARN)) {
            if (avklartefakta.getFakta().equals(VALGT_FAKTA)) {
                barnOmfattetAvNorskTrygd.add(new OmfattetFamilie(avklartefakta.getSubjekt()));
            } else {
                String begrunnelse = avklartefakta.getRegistreringer().iterator().next().getBegrunnelseKode();
                barnIkkeOmfattetAvNorskTrygd.add(
                    new IkkeOmfattetBarn(avklartefakta.getSubjekt(), begrunnelse, avklartefakta.getBegrunnelseFritekst()));
            }
        }
        return new AvklarteMedfolgendeBarn(barnOmfattetAvNorskTrygd, barnIkkeOmfattetAvNorskTrygd);
    }

    private Map<String, List<Avklartefakta>> grupperForSubjekt(Collection<Avklartefakta> avklartefakta) {
        return avklartefakta.stream()
            .collect(Collectors.groupingBy(Avklartefakta::getSubjekt, Collectors.toList()));
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void slettAvklarteFakta(long behandlingID, Avklartefaktatyper type) {
        avklarteFaktaRepository.deleteByBehandlingsresultatIdAndType(behandlingID, type);
        avklarteFaktaRepository.flush();
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagreAvklarteFakta(long behandlingID, Set<AvklartefaktaDto> avklartefaktaDtos) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException(FANT_IKKE_RESULTAT + behandlingID));

        avklarteFaktaRepository.deleteByBehandlingsresultatId(behandlingID);
        avklarteFaktaRepository.flush();

        List<Avklartefakta> avklartefaktaList = avklartefaktaDtos.stream()
            .map(avklartefaktaDto -> faktaKonverterer.opprettAvklartefaktaFraDto(avklartefaktaDto, resultat))
            .collect(Collectors.toList());

        avklarteFaktaRepository.saveAll(avklartefaktaList);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilAvklarteFakta(long behandlingID, Avklartefaktatyper type, String referanse, String subjekt, String fakta) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException(FANT_IKKE_RESULTAT + behandlingID));

        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(type);
        avklartefakta.setReferanse(referanse);
        avklartefakta.setSubjekt(subjekt);
        avklartefakta.setFakta(fakta);
        avklartefakta.setBehandlingsresultat(resultat);

        avklarteFaktaRepository.save(avklartefakta);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilAvklarteFakta(long behandlingID, Avklartefaktatyper type, String referanse, String subjekt, String fakta, String begrunnelseFritekst, String begrunnelseKode) throws IkkeFunnetException {
        Behandlingsresultat resultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException(FANT_IKKE_RESULTAT + behandlingID));

        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(type);
        avklartefakta.setReferanse(referanse);
        avklartefakta.setSubjekt(subjekt);
        avklartefakta.setFakta(fakta);
        avklartefakta.setBehandlingsresultat(resultat);
        avklartefakta.setBegrunnelseFritekst(begrunnelseFritekst);

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelseKode);
        registrering.setAvklartefakta(avklartefakta);

        avklartefakta.getRegistreringer().add(registrering);

        avklarteFaktaRepository.save(avklartefakta);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilBegrunnelse(long behandlingID, Avklartefaktatyper avklartefaktatype, String begrunnelseKode) throws IkkeFunnetException {
        leggTilAvklarteFakta(behandlingID, avklartefaktatype, avklartefaktatype.getKode(), null, begrunnelseKode);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void leggTilRegistrering(long behandlingID, Avklartefaktatyper avklartefaktatype, String begrunnelseKode) throws IkkeFunnetException {
        Avklartefakta avklartefakta = avklarteFaktaRepository.findByBehandlingsresultatIdAndType(behandlingID, avklartefaktatype)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke avklarte fakta av type " + avklartefaktatype.name() + " for behandling " + behandlingID));

        AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelseKode);
        registrering.setAvklartefakta(avklartefakta);

        avklartefakta.getRegistreringer().add(registrering);

        avklarteFaktaRepository.save(avklartefakta);
    }
}