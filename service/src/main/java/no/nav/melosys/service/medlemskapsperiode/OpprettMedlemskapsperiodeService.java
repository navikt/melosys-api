package no.nav.melosys.service.medlemskapsperiode;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.lang.String.format;
import static no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD;
import static no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A;
import static no.nav.melosys.domain.util.KodeverkUtils.tilStringCollection;

@Service
public class OpprettMedlemskapsperiodeService {
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    private final BehandlingsresultatService behandlingsresultatService;

    public OpprettMedlemskapsperiodeService(MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository, BehandlingsresultatService behandlingsresultatService) {
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public Collection<Medlemskapsperiode> utledMedlemskapsperioderFraSøknad(long behandlingID, Folketrygdloven_kap2_bestemmelser bestemmelse) throws FunksjonellException {
        if (!støtterBestemmelse(bestemmelse)) {
            throw new FunksjonellException("Støtter ikke perioder med bestemmelse " + bestemmelse);
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        validerSakstype(behandlingsresultat.getBehandling().getFagsak());
        validerVilkår(behandlingsresultat, bestemmelse);
        var medlemAvFolketrygden = hentEllerOpprettMedlemAvFolketrygden(behandlingsresultat);
        medlemAvFolketrygden.getMedlemskapsperioder().clear();

        Behandling behandling = behandlingsresultat.getBehandling();
        Behandlingsgrunnlag behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        SoeknadFtrl søknad = (SoeknadFtrl) behandlingsgrunnlag.getBehandlingsgrunnlagdata();

        var medlemskapsperioder = UtledMedlemskapsperioder.lagMedlemskapsperioder(
            new UtledMedlemskapsperioderRequest(
                søknad.periode,
                søknad.getTrygdedekning(),
                bestemmelse,
                behandlingsgrunnlag.getMottaksdato(),
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

    private void validerVilkår(Behandlingsresultat behandlingsresultat, Folketrygdloven_kap2_bestemmelser bestemmelse) throws FunksjonellException {
        var vilkårForBestemmelse = hentVilkårForBestemmelse(bestemmelse);
        if (!behandlingsresultat.oppfyllerVilkår(vilkårForBestemmelse)) {
            throw new FunksjonellException(format("Vilkår %s er påkrevd for bestemmelse %s", vilkårForBestemmelse, bestemmelse));
        }
    }

    private void validerSakstype(Fagsak fagsak) throws FunksjonellException {
        if (fagsak.getType() != Sakstyper.FTRL) {
            throw new FunksjonellException("Kan ikke opprette medlemskapsperioder for sakstype " + fagsak.getType());
        }
    }
    public Map<Folketrygdloven_kap2_bestemmelser, Collection<Vilkaar>> hentBestemmelserMedVilkaar() {
        return Map.of(
            FTRL_KAP2_2_8_FØRSTE_LEDD_A,
            Set.of(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID),
            FTRL_KAP2_2_8_ANDRE_LEDD,
            Set.of(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID, Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE)
        );
    }

    private Collection<Vilkaar> hentVilkårForBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse) throws FunksjonellException {
        return Optional.ofNullable(hentBestemmelserMedVilkaar().get(bestemmelse))
            .orElseThrow(() -> new FunksjonellException("Finner ikke vilkår for bestemmelse " + bestemmelse));
    }

    private boolean støtterBestemmelse(Folketrygdloven_kap2_bestemmelser bestemmelse) {
        return hentBestemmelserMedVilkaar().containsKey(bestemmelse);
    }

    public Collection<String> hentMuligeBegrunnelser(Vilkaar vilkår) {
        if (vilkår == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE) {
            return tilStringCollection(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.values());
        }

        return Collections.emptyList();
    }
}
