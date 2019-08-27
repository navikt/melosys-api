package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsresultatService {

    private static final Logger log = LoggerFactory.getLogger(BehandlingsresultatService.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    public BehandlingsresultatService(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Transactional
    public void tømBehandlingsresultat(long behandlingsid) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingsid).orElse(null);
        if (behandlingsresultat != null) {
            log.info("Fjerner avklarte fakta, lovvalgsperioder og vilkårsresultater fra behandlingsresultat med behandlingsid: {} ", behandlingsid);
            behandlingsresultat.getAvklartefakta().clear();
            behandlingsresultat.getLovvalgsperioder().clear();
            behandlingsresultat.getVilkaarsresultater().clear();
            behandlingsresultatRepository.save(behandlingsresultat);
        }
    }

    public Behandlingsresultat hentBehandlingsresultat(long behandlingsid) throws IkkeFunnetException {
        return behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException("Kan ikke finne behandlingsresultat for behandling: " + behandlingsid));
    }

    @Transactional(rollbackFor = Exception.class)
    public void replikerBehandlingsresultat(Behandling tidligsteInaktiveBehandling, Behandling behandlingsreplika)
        throws IkkeFunnetException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(tidligsteInaktiveBehandling.getId());

        Behandlingsresultat behandlingsresultatsreplika = (Behandlingsresultat) BeanUtils.cloneBean(behandlingsresultat);
        behandlingsresultatsreplika.setBehandling(behandlingsreplika);
        behandlingsresultatsreplika.setId(null);

        replikerAvklartefakta(behandlingsresultat, behandlingsresultatsreplika);
        replikerLovvalgsperioder(behandlingsresultat, behandlingsresultatsreplika);
        replikerVilkaarsresultat(behandlingsresultat, behandlingsresultatsreplika);
        replikerBehandlingsresultatBegrunnelser(behandlingsresultat, behandlingsresultatsreplika);

        behandlingsresultatRepository.save(behandlingsresultatsreplika);
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
                avklartefaktareplika.getRegistreringer().add(avklartefaktaRegistreringreplika);
            }
            behandlingsresultatsreplika.getAvklartefakta().add(avklartefaktareplika);
        }
    }

    public void oppdaterBehandlingsresultattype(Long id, Behandlingsresultattyper behandlingsresultattype) {
        Optional<Behandlingsresultat> optionalBehandlingsresultat = behandlingsresultatRepository.findById(id);
        if (optionalBehandlingsresultat.isPresent()){
            Behandlingsresultat behandlingsresultat = optionalBehandlingsresultat.get();
            log.info("Setter behandlingsresultattype på {} til {}", id, behandlingsresultattype);
            behandlingsresultat.setType(behandlingsresultattype);
            behandlingsresultatRepository.save(behandlingsresultat);
        }
    }

    public void oppdaterBehandlingsMaate(Long id, Behandlingsmaate behandlingsmaate) throws FunksjonellException {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(id);
        if (behandlingsresultat.getBehandlingsmåte() != null && behandlingsresultat.getBehandlingsmåte() != Behandlingsmaate.UDEFINERT) {
            throw new FunksjonellException("Behandlingsmaate kan ikke oppdateres etter det er definert!");
        }

        behandlingsresultat.setBehandlingsmåte(behandlingsmaate);
        behandlingsresultatRepository.save(behandlingsresultat);
    }
}
