package no.nav.melosys.service.behandling;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsresultatService {
    private static final Logger log = LoggerFactory.getLogger(BehandlingsresultatService.class);
    //TODO: Ha generisk toppklasse?
    public static final String KAN_IKKE_FINNE_BEHANDLINGSRESULTAT = "Kan ikke finne behandlingsresultat for behandling: ";

    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BehandlingsresultatService(BehandlingsresultatRepository behandlingsresultatRepository, VilkaarsresultatService vilkaarsresultatService) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Transactional
    public void tømBehandlingsresultat(long behandlingID) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingID));

        log.info("Fjerner avklarte fakta, lovvalgsperioder, medlemAvFolketrygden og vilkårsresultater fra behandlingsresultat med behandlingID: {} ", behandlingID);
        behandlingsresultat.getAvklartefakta().clear();
        behandlingsresultat.getLovvalgsperioder().clear();
        fjernMedlemAvFolketrygdenHvisDenFinnes(behandlingsresultat);
        behandlingsresultat.setMedlemAvFolketrygden(null);
        behandlingsresultat.setUtfallRegistreringUnntak(null);
        behandlingsresultat.setBegrunnelseFritekst(null);
        behandlingsresultat.setInnledningFritekst(null);
        behandlingsresultat.setNyVurderingBakgrunn(null);
        behandlingsresultat.setTrygdeavgiftFritekst(null);
        vilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(behandlingID);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    public Behandlingsresultat hentBehandlingsresultat(long behandlingsid) {
        return behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    public List<Behandlingsresultat> finnAlleBehandlingsresultatMedFakturaserieReferanse(String fakturaserieReferanse) {
        return behandlingsresultatRepository.findAllByFakturaserieReferanse(fakturaserieReferanse);
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

    public Behandlingsresultat lagre(Behandlingsresultat resultat) {
        return behandlingsresultatRepository.save(resultat);
    }

    public void lagreNyttBehandlingsresultat(Behandling behandling) {
        Behandlingsresultat nyttBehandlingsresultat = new Behandlingsresultat();
        nyttBehandlingsresultat.setBehandling(behandling);
        nyttBehandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        nyttBehandlingsresultat.setBehandlingsmåte(Behandlingsmaate.MANUELT);
        behandlingsresultatRepository.save(nyttBehandlingsresultat);
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

        behandlingsresultat.setType(finnKorrektBehandlingsResultat(utfallRegistreringUnntak));
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

        behandlingsresultat.setUtfallUtpeking(utfallUtpeking);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    private static Behandlingsresultattyper finnKorrektBehandlingsResultat(Utfallregistreringunntak utfallregistreringunntak) {
        if (utfallregistreringunntak.equals(Utfallregistreringunntak.GODKJENT) || utfallregistreringunntak.equals(Utfallregistreringunntak.DELVIS_GODKJENT)) {
            return (Behandlingsresultattyper.REGISTRERT_UNNTAK);
        } else if (utfallregistreringunntak.equals(Utfallregistreringunntak.IKKE_GODKJENT)) {
            return (Behandlingsresultattyper.FERDIGBEHANDLET);
        }
        return Behandlingsresultattyper.IKKE_FASTSATT;
    }

    public void oppdaterBegrunnelser(long behandlingID, Set<BehandlingsresultatBegrunnelse> begrunnelser, String begrunnelseFritekst) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        begrunnelser.forEach(b -> b.setBehandlingsresultat(behandlingsresultat));
        behandlingsresultat.getBehandlingsresultatBegrunnelser().addAll(begrunnelser);
        behandlingsresultat.setBegrunnelseFritekst(begrunnelseFritekst);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    public Behandlingsresultat oppdaterFritekster(long behandlingID, String begrunnelseFritekst, String innledningFritekst, String trygdeavgiftFritekst) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setBegrunnelseFritekst(begrunnelseFritekst);
        behandlingsresultat.setInnledningFritekst(innledningFritekst);
        behandlingsresultat.setTrygdeavgiftFritekst(trygdeavgiftFritekst);
        return behandlingsresultatRepository.save(behandlingsresultat);
    }

    public Behandlingsresultat oppdaterNyVurderingBakgrunn(long behandlingID, String nyVurderingBakgrunn) {
        final Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);

        behandlingsresultat.setNyVurderingBakgrunn(nyVurderingBakgrunn);

        return behandlingsresultatRepository.save(behandlingsresultat);
    }

    private void fjernMedlemAvFolketrygdenHvisDenFinnes(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.getMedlemAvFolketrygden() != null && behandlingsresultat.getMedlemAvFolketrygden().getFastsattTrygdeavgift() != null) {
            behandlingsresultat.getMedlemAvFolketrygden().getFastsattTrygdeavgift().getTrygdeavgiftsperioder().clear();
            behandlingsresultatRepository.saveAndFlush(behandlingsresultat);
        }
    }
}
