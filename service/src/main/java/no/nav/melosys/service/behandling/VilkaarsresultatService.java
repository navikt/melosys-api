package no.nav.melosys.service.behandling;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.behandling.BehandlingsresultatService.KAN_IKKE_FINNE_BEHANDLINGSRESULTAT;

@Service
public class VilkaarsresultatService {

    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public static final Collection<Vilkaar> IMMUTABLE_VILKAAR = Collections.singleton(FO_883_2004_INNGANGSVILKAAR);

    public VilkaarsresultatService(
        BehandlingsresultatRepository behandlingsresultatRepository,
        SaksbehandlingRegler saksbehandlingRegler) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    public Behandlingsresultat hentBehandlingsresultat(long behandlingsid) {
        return behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow(() -> new IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid));
    }

    @Transactional(readOnly = true)
    public List<VilkaarDto> hentVilkaar(long behandlingID) {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);

        List<VilkaarDto> vilkaarDtoListe = new ArrayList<>();
        for (Vilkaarsresultat vilkaarsresultat : behandlingsresultat.getVilkaarsresultater()) {
            VilkaarDto vilkaarDto = new VilkaarDto();
            vilkaarDto.setVilkaar(vilkaarsresultat.getVilkaar().getKode());
            vilkaarDto.setOppfylt(vilkaarsresultat.isOppfylt());
            vilkaarDto.setBegrunnelseKoder(vilkaarsresultat.getBegrunnelser().stream().map(VilkaarBegrunnelse::getKode).collect(Collectors.toSet()));
            vilkaarDto.setBegrunnelseFritekst(vilkaarsresultat.getBegrunnelseFritekst());
            vilkaarDto.setBegrunnelseFritekstEngelsk(vilkaarsresultat.getBegrunnelseFritekstEessi());
            vilkaarDtoListe.add(vilkaarDto);
        }

        return vilkaarDtoListe;
    }

    @Transactional(readOnly = true)
    public Optional<Vilkaarsresultat> finnVilkaarsresultat(long behandlingID, Vilkaar vilkaar) {
        return hentBehandlingsresultat(behandlingID).getVilkaarsresultater().stream()
            .filter(vilkaarsresultat -> vilkaarsresultat.getVilkaar().equals(vilkaar))
            .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean oppfyllerVilkaar(long behandlingID, Vilkaar vilkaar) {
        return hentBehandlingsresultat(behandlingID).getVilkaarsresultater().stream()
            .anyMatch(vilkaarsresultat -> vilkaar.equals(vilkaarsresultat.getVilkaar()) && vilkaarsresultat.isOppfylt());
    }

    @Transactional(readOnly = true)
    public boolean harVilkaarForArtikkel12(long behandlingID) {
        Optional<Vilkaarsresultat> art121Vilkaar = finnVilkaarsresultat(behandlingID, FO_883_2004_ART12_1);
        Optional<Vilkaarsresultat> art122Vilkaar = finnVilkaarsresultat(behandlingID, FO_883_2004_ART12_2);
        return art121Vilkaar.isPresent() || art122Vilkaar.isPresent();
    }

    @Transactional(readOnly = true)
    public boolean harVilkaarForArtikkel16(long behandlingID) {
        Optional<Vilkaarsresultat> art161Vilkaar = finnVilkaarsresultat(behandlingID, FO_883_2004_ART16_1);
        return art161Vilkaar.isPresent();
    }

    @Transactional
    public void registrerVilkår(long behandlingID, List<VilkaarDto> vilkaarDtoer) {
        validerVilkår(vilkaarDtoer);
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        tømVilkårsresultatFraBehandlingsresultat(behandlingID);
        // Flush fordi vi potensielt legger til samme vilkåret igjen. INSERT kommer før DELETE i Hibernate, som skaper UNIQUE constraint problemer uten flush.
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat);

        for (VilkaarDto vilkaarDto : vilkaarDtoer) {
            Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultat(
                behandlingsresultat,
                Vilkaar.valueOf(vilkaarDto.getVilkaar()),
                vilkaarDto.isOppfylt(),
                vilkaarDto.getBegrunnelseKoder(),
                vilkaarDto.getBegrunnelseFritekst(),
                vilkaarDto.getBegrunnelseFritekstEngelsk());
            behandlingsresultat.getVilkaarsresultater().add(vilkaarsresultat);
        }
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    @Transactional
    public void tømVilkårsresultatFraBehandlingsresultat(long behandlingID) {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);

        var behandling = behandlingsresultat.getBehandling();
        var fagsak = behandling.getFagsak();
        if (fagsak.erSakstypeEøs() && !saksbehandlingRegler.harIngenFlyt(behandling)) {
            behandlingsresultat.getVilkaarsresultater().removeIf(vilkaarsresultat -> !IMMUTABLE_VILKAAR.contains(vilkaarsresultat.getVilkaar()));
        } else {
            behandlingsresultat.getVilkaarsresultater().clear();
        }
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat);
    }

    private void validerVilkår(List<VilkaarDto> vilkaarDtoer) {

        final Collection<String> nyeVilkår = vilkaarDtoer.stream().map(VilkaarDto::getVilkaar).toList();

        for (Vilkaar immutableVilkår : IMMUTABLE_VILKAAR) {
            if (nyeVilkår.contains(immutableVilkår.getKode())) {
                throw new FunksjonellException("Kan ikke endre vilkår " + immutableVilkår);
            }
        }
    }

    @Transactional
    public void oppdaterVilkaarsresultat(long behandlingID,
                                         Vilkaar vilkaar,
                                         boolean oppfylt,
                                         Set<Kodeverk> begrunnelseKoder) {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingID);
        behandlingsresultat.getVilkaarsresultater().clear();
        // Flush fordi vi potensielt legger til samme vilkåret igjen. INSERT kommer før DELETE i Hibernate, som skaper UNIQUE constraint problemer uten flush.
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat);
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultat(
            behandlingsresultat,
            vilkaar,
            oppfylt,
            begrunnelseKoder.stream().map(Kodeverk::getKode).collect(Collectors.toSet()));

        behandlingsresultat.getVilkaarsresultater().add(vilkaarsresultat);
        behandlingsresultatRepository.save(behandlingsresultat);
    }

    private Vilkaarsresultat lagVilkaarsresultat(Behandlingsresultat behandlingsresultat,
                                                 Vilkaar vilkaar,
                                                 boolean oppfylt,
                                                 Set<String> begrunnelseKoder) {
        return lagVilkaarsresultat(behandlingsresultat, vilkaar, oppfylt, begrunnelseKoder,
            null, null);
    }

    private Vilkaarsresultat lagVilkaarsresultat(Behandlingsresultat behandlingsresultat,
                                                 Vilkaar vilkaar,
                                                 boolean oppfylt,
                                                 Set<String> begrunnelseKoder,
                                                 String begrunnelseFritekst,
                                                 String begrunnelseFritekstEngelsk) {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBehandlingsresultat(behandlingsresultat);
        vilkaarsresultat.setVilkaar(vilkaar);
        vilkaarsresultat.setOppfylt(oppfylt);
        vilkaarsresultat.setBegrunnelser(begrunnelseKoder.stream().map(kode -> lagBegrunnelse(vilkaarsresultat, kode))
            .collect(Collectors.toSet()));
        vilkaarsresultat.setBegrunnelseFritekst(begrunnelseFritekst);
        vilkaarsresultat.setBegrunnelseFritekstEessi(begrunnelseFritekstEngelsk);
        return vilkaarsresultat;
    }

    private VilkaarBegrunnelse lagBegrunnelse(Vilkaarsresultat vilkaarsresultat, String begrunnelseKode) {
        VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
        begrunnelse.setVilkaarsresultat(vilkaarsresultat);
        begrunnelse.setKode(begrunnelseKode);
        return begrunnelse;
    }
}
