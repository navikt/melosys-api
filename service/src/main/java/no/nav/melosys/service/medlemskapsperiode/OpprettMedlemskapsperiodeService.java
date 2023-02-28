package no.nav.melosys.service.medlemskapsperiode;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadFtrl;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.lang.String.format;
import static no.nav.melosys.domain.util.KodeverkUtils.tilStringCollection;

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
    public Collection<Medlemskapsperiode> utledMedlemskapsperioderFraSøknad(long behandlingID, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (!støtterBestemmelse(bestemmelse, behandlingsresultat.getBehandling().getTema())) {
            throw new FunksjonellException("Støtter ikke perioder med bestemmelse " + bestemmelse);
        }

        validerSakstype(behandlingsresultat.getBehandling().getFagsak());
        validerVilkår(behandlingsresultat, bestemmelse);
        var medlemAvFolketrygden = hentEllerOpprettMedlemAvFolketrygden(behandlingsresultat);
        medlemAvFolketrygden.getMedlemskapsperioder().clear();

        Behandling behandling = behandlingsresultat.getBehandling();
        SoeknadFtrl søknad = (SoeknadFtrl) behandling.getMottatteOpplysninger().getMottatteOpplysningerData();

        var medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioder(
            new UtledMedlemskapsperioderRequest(
                søknad.periode,
                søknad.getTrygdedekning(),
                bestemmelse,
                utledMottaksdato.getMottaksdato(behandling),
                søknad.soeknadsland.landkoder.stream().collect(onlyElement())
            )
        );

        medlemAvFolketrygden.getMedlemskapsperioder().addAll(medlemskapsperioder);
        medlemskapsperioder.forEach(m -> m.setMedlemAvFolketrygden(medlemAvFolketrygden));
        return medlemAvFolketrygdenRepository.save(medlemAvFolketrygden).getMedlemskapsperioder();
    }

    private MedlemAvFolketrygden hentEllerOpprettMedlemAvFolketrygden(Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.getMedlemAvFolketrygden() != null) {
            return behandlingsresultat.getMedlemAvFolketrygden();
        }

        var medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setBehandlingsresultat(behandlingsresultat);
        return medlemAvFolketrygdenRepository.save(medlemAvFolketrygden);
    }

    private void validerVilkår(Behandlingsresultat behandlingsresultat, Folketrygdloven_kap2_bestemmelser bestemmelse) {
        var vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse, behandlingsresultat.getBehandling().getTema());
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw new FunksjonellException(format("Vilkår %s er påkrevd for bestemmelse %s", vilkårForBestemmelse, bestemmelse));
        }
    }

    private void validerSakstype(Fagsak fagsak) {
        if (fagsak.getType() != Sakstyper.FTRL) {
            throw new FunksjonellException("Kan ikke opprette medlemskapsperioder for sakstype " + fagsak.getType());
        }
    }

    public Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> hentBestemmelserMedVilkaar(Behandlingstema behandlingstema, Boolean kunStøttet) {
        return new UtledBestemmelserOgVilkaar().hentRiktigBestemmelserOgVilkår(behandlingstema, kunStøttet);
    }

    private Collection<Vilkaar> hentVilkårForBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse, Behandlingstema behandlingstema) {
        return Optional.ofNullable(hentBestemmelserMedVilkaar(behandlingstema, true).get(bestemmelse))
            .orElseThrow(() -> new FunksjonellException("Finner ikke vilkår for bestemmelse " + bestemmelse));
    }

    private boolean støtterBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse, Behandlingstema behandlingstema) {
        return hentBestemmelserMedVilkaar(behandlingstema, true).containsKey(bestemmelse);
    }

    public Collection<String> hentMuligeBegrunnelser(Vilkaar vilkår) {
        if (vilkår == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE) {
            return tilStringCollection(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values());
        }

        return Collections.emptyList();
    }
}
