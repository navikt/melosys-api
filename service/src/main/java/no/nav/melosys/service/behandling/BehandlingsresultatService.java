package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsresultatService {
    private static final Logger log = LoggerFactory.getLogger(BehandlingsresultatService.class);
    private static final String KAN_IKKE_FINNE_BEHANDLINGSRESULTAT = "Kan ikke finne behandlingsresultat for behandling: ";

    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BehandlingsresultatService(BehandlingsresultatRepository behandlingsresultatRepository,
                                      @Lazy VilkaarsresultatService vilkaarsresultatService) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Transactional
    public void tømBehandlingsresultat(long behandlingsid) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingsid).orElse(null);
        if (behandlingsresultat != null) {
            log.info("Fjerner avklarte fakta, lovvalgsperioder, medlemAvFolketrygden og vilkårsresultater fra behandlingsresultat med behandlingsid: {} ", behandlingsid);
            behandlingsresultat.getAvklartefakta().clear();
            behandlingsresultat.getLovvalgsperioder().clear();
            behandlingsresultat.setMedlemAvFolketrygden(null);
            behandlingsresultat.setUtfallRegistreringUnntak(null);
            behandlingsresultat.setBegrunnelseFritekst(null);
            behandlingsresultat.setInnledningFritekst(null);
            vilkaarsresultatService.tømVilkårForBehandlingsresultat(behandlingsresultat);
            behandlingsresultatRepository.save(behandlingsresultat);
        }
    }

    public Behandlingsresultat hentBehandlingsresultat(long behandlingsid) {
        return behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    public Behandlingsresultat hentBehandlingsresultatMedAnmodningsperioder(long behandlingsid) {
        return behandlingsresultatRepository.findWithAnmodningsperioderById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    public Behandlingsresultat hentBehandlingsresultatMedLovvalgsperioder(long behandlingsid) {
        return behandlingsresultatRepository.findWithLovvalgsperioderById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    public Behandlingsresultat hentBehandlingsresultatMedKontrollresultat(long behandlingsid) {
        return behandlingsresultatRepository.findWithKontrollresultaterById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    public Behandlingsresultat hentBehandlingsresultatMedAvklartefakta(long behandlingsid) {
        return behandlingsresultatRepository.findWithAvklartefaktaById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    public void lagre(Behandlingsresultat resultat) {
        behandlingsresultatRepository.save(resultat);
    }

    public void lagreNyttBehandlingsresultat(Behandling behandling) {
        Behandlingsresultat nyttBehandlingsresultat = new Behandlingsresultat();
        nyttBehandlingsresultat.setBehandling(behandling);
        nyttBehandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        nyttBehandlingsresultat.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        behandlingsresultatRepository.save(nyttBehandlingsresultat);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replikerBehandlingsresultat(Behandling tidligsteInaktiveBehandling, Behandling behandlingsreplika)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(tidligsteInaktiveBehandling.getId());

        Behandlingsresultat behandlingsresultatsreplika = (Behandlingsresultat) BeanUtils.cloneBean(behandlingsresultat);
        behandlingsresultatsreplika.setBehandling(behandlingsreplika);
        behandlingsresultatsreplika.setId(null);
        behandlingsresultatsreplika.setVedtakMetadata(null);
        behandlingsresultatsreplika.setUtfallRegistreringUnntak(null);
        behandlingsresultatsreplika.setUtfallUtpeking(null);
        behandlingsresultatsreplika.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        behandlingsresultatsreplika.setType(Behandlingsresultattyper.IKKE_FASTSATT);

        replikerAvklartefakta(behandlingsresultat, behandlingsresultatsreplika);
        replikerLovvalgsperioder(behandlingsresultat, behandlingsresultatsreplika);
        replikerVilkaarsresultat(behandlingsresultat, behandlingsresultatsreplika);
        replikerAnmodningsperioder(behandlingsresultat, behandlingsresultatsreplika);
        replikerBehandlingsresultatBegrunnelser(behandlingsresultat, behandlingsresultatsreplika);
        replikerKontrollResultater(behandlingsresultat, behandlingsresultatsreplika);
        replikerUtpekingsperioder(behandlingsresultat, behandlingsresultatsreplika);

        behandlingsresultatRepository.save(behandlingsresultatsreplika);
    }

    private void replikerUtpekingsperioder(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        behandlingsresultatsreplika.setUtpekingsperioder(new HashSet<>());
        for (Utpekingsperiode utpekingsperiode : behandlingsresultat.getUtpekingsperioder()) {
            Utpekingsperiode utpekingsperiodereplika = (Utpekingsperiode) BeanUtils.cloneBean(utpekingsperiode);
            utpekingsperiodereplika.setBehandlingsresultat(behandlingsresultatsreplika);
            utpekingsperiodereplika.setId(null);
            utpekingsperiodereplika.setMedlPeriodeID(null);
            utpekingsperiodereplika.setSendtUtland(null);
            behandlingsresultatsreplika.getUtpekingsperioder().add(utpekingsperiodereplika);
        }
    }

    private void replikerAnmodningsperioder(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        behandlingsresultatsreplika.setAnmodningsperioder(new HashSet<>());
        for (Anmodningsperiode anmodningsperiodeOrig : behandlingsresultat.getAnmodningsperioder()) {
            Anmodningsperiode anmodningsperiodereplika = (Anmodningsperiode) BeanUtils.cloneBean(anmodningsperiodeOrig);
            anmodningsperiodereplika.setBehandlingsresultat(behandlingsresultatsreplika);
            anmodningsperiodereplika.setId(null);
            anmodningsperiodereplika.setMedlPeriodeID(null);
            anmodningsperiodereplika.setSendtUtland(false);
            anmodningsperiodereplika.setAnmodningsperiodeSvar(null);
            behandlingsresultatsreplika.getAnmodningsperioder().add(anmodningsperiodereplika);
        }
    }

    private void replikerVilkaarsresultat(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika)
        throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        behandlingsresultatsreplika.setVilkaarsresultater(new HashSet<>());
        for (Vilkaarsresultat vilkaarsresultatOrig : behandlingsresultat.getVilkaarsresultater()) {
            Vilkaarsresultat vilkaarsresultatreplika = (Vilkaarsresultat) BeanUtils.cloneBean(vilkaarsresultatOrig);
            vilkaarsresultatreplika.setBehandlingsresultat(behandlingsresultatsreplika);
            vilkaarsresultatreplika.setId(null);
            vilkaarsresultatreplika.setBegrunnelser(new HashSet<>());
            for (VilkaarBegrunnelse vilkaarBegrunnelseOrig : vilkaarsresultatOrig.getBegrunnelser()) {
                VilkaarBegrunnelse vilkaarBegrunnelsesreplika = (VilkaarBegrunnelse) BeanUtils.cloneBean(vilkaarBegrunnelseOrig);
                vilkaarBegrunnelsesreplika.setId(null);
                vilkaarBegrunnelsesreplika.setVilkaarsresultat(vilkaarsresultatreplika);
                vilkaarsresultatreplika.getBegrunnelser().add(vilkaarBegrunnelsesreplika);
            }
            behandlingsresultatsreplika.getVilkaarsresultater().add(vilkaarsresultatreplika);
        }
    }

    private void replikerLovvalgsperioder(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika)
        throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        behandlingsresultatsreplika.setLovvalgsperioder(new HashSet<>());
        for (Lovvalgsperiode lovvalgsperiodeOrig : behandlingsresultat.getLovvalgsperioder()) {
            Lovvalgsperiode lovvalgsperiodereplika = (Lovvalgsperiode) BeanUtils.cloneBean(lovvalgsperiodeOrig);
            lovvalgsperiodereplika.setBehandlingsresultat(behandlingsresultatsreplika);
            lovvalgsperiodereplika.setId(null);
            behandlingsresultatsreplika.getLovvalgsperioder().add(lovvalgsperiodereplika);
        }
    }

    private void replikerBehandlingsresultatBegrunnelser(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika)
        throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        behandlingsresultatsreplika.setBehandlingsresultatBegrunnelser(new HashSet<>());
        for (BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelseOrig : behandlingsresultat.getBehandlingsresultatBegrunnelser()) {
            BehandlingsresultatBegrunnelse behandlingsresultatBegrunnelsesreplika = (BehandlingsresultatBegrunnelse) BeanUtils.cloneBean(behandlingsresultatBegrunnelseOrig);
            behandlingsresultatBegrunnelsesreplika.setBehandlingsresultat(behandlingsresultatsreplika);
            behandlingsresultatBegrunnelsesreplika.setId(null);
            behandlingsresultatBegrunnelsesreplika.setKode(behandlingsresultatBegrunnelseOrig.getKode());
            behandlingsresultatsreplika.getBehandlingsresultatBegrunnelser().add(behandlingsresultatBegrunnelsesreplika);
        }
    }

    private void replikerAvklartefakta(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika)
        throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        behandlingsresultatsreplika.setAvklartefakta(new HashSet<>());
        for (Avklartefakta avklartefaktaOrig : behandlingsresultat.getAvklartefakta()) {
            Avklartefakta avklartefaktareplika = (Avklartefakta) BeanUtils.cloneBean(avklartefaktaOrig);
            avklartefaktareplika.setBehandlingsresultat(behandlingsresultatsreplika);
            avklartefaktareplika.setId(null);
            avklartefaktareplika.setRegistreringer(new HashSet<>());
            for (AvklartefaktaRegistrering avklartefaktaRegistreringOrig : avklartefaktaOrig.getRegistreringer()) {
                AvklartefaktaRegistrering avklartefaktaRegistreringreplika = (AvklartefaktaRegistrering) BeanUtils.cloneBean(avklartefaktaRegistreringOrig);
                avklartefaktaRegistreringreplika.setId(null);
                avklartefaktaRegistreringreplika.setAvklartefakta(avklartefaktareplika);
                avklartefaktareplika.getRegistreringer().add(avklartefaktaRegistreringreplika);
            }
            behandlingsresultatsreplika.getAvklartefakta().add(avklartefaktareplika);
        }
    }

    private void replikerKontrollResultater(Behandlingsresultat behandlingsresultat, Behandlingsresultat behandlingsresultatsreplika) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        behandlingsresultatsreplika.setKontrollresultater(new HashSet<>());
        for (var kontrollresultatOrig : behandlingsresultat.getKontrollresultater()) {
            Kontrollresultat kontrollresultatreplika = (Kontrollresultat) BeanUtils.cloneBean(kontrollresultatOrig);
            kontrollresultatreplika.setId(null);
            kontrollresultatreplika.setBehandlingsresultat(behandlingsresultatsreplika);
            behandlingsresultatsreplika.getKontrollresultater().add(kontrollresultatreplika);
        }
    }

    public void oppdaterBehandlingsresultattype(Long id, Behandlingsresultattyper behandlingsresultattype) {
        Optional<Behandlingsresultat> optionalBehandlingsresultat = behandlingsresultatRepository.findById(id);
        if (optionalBehandlingsresultat.isPresent()) {
            Behandlingsresultat behandlingsresultat = optionalBehandlingsresultat.get();
            log.info("Setter behandlingsresultattype på {} til {}", id, behandlingsresultattype);
            behandlingsresultat.setType(behandlingsresultattype);
            behandlingsresultatRepository.save(behandlingsresultat);
        }
    }

    public void oppdaterBehandlingsMaate(Long id, Behandlingsmaate behandlingsmaate) {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(id);

        behandlingsresultat.setBehandlingsmåte(behandlingsmaate);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    public void settUtfallRegistreringUnntakOgType(long behandlingID, Utfallregistreringunntak utfallRegistreringUnntak) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        if (behandlingsresultat.getUtfallRegistreringUnntak() != null) {
            throw new FunksjonellException("Utfall for registrering av unntak er allerede satt for behandlingsresultat " + behandlingID);
        }

        behandlingsresultat.setType(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        oppdaterUtfallRegistreringUnntak(behandlingID, utfallRegistreringUnntak);
    }

    public Behandlingsresultat oppdaterUtfallRegistreringUnntak(long behandlingID, Utfallregistreringunntak utfallUtpeking) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultatMedKontrollresultat(behandlingID);
        behandlingsresultat.setUtfallRegistreringUnntak(utfallUtpeking);
        return behandlingsresultatRepository.save(behandlingsresultat);
    }

    public void oppdaterUtfallUtpeking(long behandlingID, Utfallregistreringunntak utfallUtpeking) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        if (behandlingsresultat.getUtfallUtpeking() != null) {
            throw new FunksjonellException("Utfall for utpeking er allerede satt for behandlingsresultat " + behandlingID);
        }

        //behandlingsresultat.setType(??); FIXME
        behandlingsresultat.setUtfallUtpeking(utfallUtpeking);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    public void oppdaterBegrunnelser(long behandlingID, Set<BehandlingsresultatBegrunnelse> begrunnelser, String begrunnelseFritekst) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        begrunnelser.forEach(b -> b.setBehandlingsresultat(behandlingsresultat));
        behandlingsresultat.getBehandlingsresultatBegrunnelser().addAll(begrunnelser);
        behandlingsresultat.setBegrunnelseFritekst(begrunnelseFritekst);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    public Behandlingsresultat oppdaterFritekster(long behandlingID, String begrunnelseFritekst, String innledningFritekst) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setBegrunnelseFritekst(begrunnelseFritekst);
        behandlingsresultat.setInnledningFritekst(innledningFritekst);
        return behandlingsresultatRepository.save(behandlingsresultat);
    }
}
