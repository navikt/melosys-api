package no.nav.melosys.service.medlemskapsperiode;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;

@Service
public class OpprettMedlemskapsperiodeService {
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UtledMottaksdato utledMottaksdato;

    public OpprettMedlemskapsperiodeService(MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository,
                                            BehandlingsresultatService behandlingsresultatService,
                                            UtledMottaksdato utledMottaksdato) {
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
        this.behandlingsresultatService = behandlingsresultatService;
        this.utledMottaksdato = utledMottaksdato;
    }

    @Transactional
    public Collection<Medlemskapsperiode> opprettForslagPåMedlemskapsperioder(long behandlingID, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        var behandling = behandlingsresultat.getBehandling();
        var medlemAvFolketrygden = hentEllerOpprettMedlemAvFolketrygden(behandlingsresultat);

        validerSakstype(behandling.getFagsak());
        validerBestemmelse(bestemmelse, behandling.getTema());
        validerVilkår(behandlingsresultat, bestemmelse);

        if (medlemAvFolketrygden.getMedlemskapsperioder().isEmpty()) {
            Collection<Medlemskapsperiode> medlemskapsperioder;
            var opprinneligBehandling = behandling.getOpprinneligBehandling();

            if ((behandling.erNyVurdering() || behandling.erManglendeInnbetalingTrygdeavgift()) && opprinneligBehandling != null) {
                var opprinneligBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.getId());
                medlemskapsperioder = new UtledMedlemskapsperioder().lagMedlemskapsperioderForAndregangsbehandling(opprinneligBehandlingsresultat, bestemmelse, behandling.getType());
            } else {
                SøknadNorgeEllerUtenforEØS søknad = (SøknadNorgeEllerUtenforEØS) behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
                medlemskapsperioder = new UtledMedlemskapsperioder().lagMedlemskapsperioder(
                    new UtledMedlemskapsperioderDto(
                        søknad.periode,
                        søknad.getTrygdedekning(),
                        utledMottaksdato.getMottaksdato(behandling),
                        søknad.hentArbeidsland(),
                        bestemmelse)
                );
            }
            medlemAvFolketrygden.getMedlemskapsperioder().addAll(medlemskapsperioder);
            medlemskapsperioder.forEach(m -> m.setMedlemAvFolketrygden(medlemAvFolketrygden));
        } else {
            medlemAvFolketrygden.getMedlemskapsperioder().forEach(medlemskapsperiode -> {
                if (!medlemskapsperiode.erOpphørt()) {
                    medlemskapsperiode.setBestemmelse(bestemmelse);
                }
            });
        }

        return medlemAvFolketrygdenRepository.save(medlemAvFolketrygden).getMedlemskapsperioder();
    }


    private MedlemAvFolketrygden hentEllerOpprettMedlemAvFolketrygden(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.getMedlemAvFolketrygden() != null) {
            return behandlingsresultat.getMedlemAvFolketrygden();
        }

        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setBehandlingsresultat(behandlingsresultat);
        return medlemAvFolketrygden;
    }

    private void validerSakstype(Fagsak fagsak) {
        if (!fagsak.erSakstypeFtrl()) {
            throw new FunksjonellException("Kan ikke opprette medlemskapsperioder for sakstype " + fagsak.getType());
        }
    }

    private void validerBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse, Behandlingstema behandlingstema) {
        if (bestemmelse == null) {
            throw new FunksjonellException("Bestemmelse er ikke satt. Krever bestemmelse ved opprettelse av forslag for medlemskapsperioder.");
        }
        var støttedeBestemmelser = hentStøttedeBestemmelserMedVilkår(behandlingstema);
        if (!støttedeBestemmelser.containsKey(bestemmelse)) {
            throw new FunksjonellException("Støtter ikke perioder med bestemmelse " + bestemmelse + " for behandlingstema " + behandlingstema);
        }
    }

    private void validerVilkår(Behandlingsresultat behandlingsresultat, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        var vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse, behandlingsresultat.getBehandling().getTema());
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw new FunksjonellException(format("Vilkår %s er påkrevd for bestemmelse %s", vilkårForBestemmelse, bestemmelse));
        }
    }

    public Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> hentStøttedeBestemmelserMedVilkår(Behandlingstema behandlingstema) {
        return new UtledBestemmelserOgVilkår().hentStøttedeBestemmelserOgVilkår(behandlingstema);
    }

    private Collection<Vilkaar> hentVilkårForBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse, Behandlingstema behandlingstema) {
        return Optional.ofNullable(hentStøttedeBestemmelserMedVilkår(behandlingstema).get(bestemmelse))
            .orElseThrow(() -> new FunksjonellException("Finner ikke vilkår for bestemmelse " + bestemmelse));
    }
}
