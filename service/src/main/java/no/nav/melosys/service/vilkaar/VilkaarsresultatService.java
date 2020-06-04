package no.nav.melosys.service.vilkaar;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;

@Service
public class VilkaarsresultatService {
    private final BehandlingsresultatService behandlingsresultatService;
    private final VilkaarsresultatRepository vilkaarsresultatRepo;

    private static final Collection<Vilkaar> IMMUTABLE_VILKAAR = Collections.singleton(FO_883_2004_INNGANGSVILKAAR);

    @Autowired
    public VilkaarsresultatService(BehandlingsresultatService behandlingsresultatService,
                                   VilkaarsresultatRepository vilkaarsresultatRepo) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.vilkaarsresultatRepo = vilkaarsresultatRepo;
    }

    @Transactional(readOnly = true)
    public List<VilkaarDto> hentVilkaar(long behandlingID) {
        List<Vilkaarsresultat> vilkaarsresultatListe = vilkaarsresultatRepo.findByBehandlingsresultatId(behandlingID);

        List<VilkaarDto> vilkaarDtoListe = new ArrayList<>();
        for (Vilkaarsresultat vilkaarsresultat : vilkaarsresultatListe) {
            VilkaarDto vilkaarDto = new VilkaarDto();
            vilkaarDto.setVilkaar(vilkaarsresultat.getVilkaar().getKode());
            vilkaarDto.setOppfylt(vilkaarsresultat.isOppfylt());
            vilkaarDto.setBegrunnelseKoder(vilkaarsresultat.getBegrunnelser().stream().map(VilkaarBegrunnelse::getKode).collect(Collectors.toList()));
            vilkaarDto.setBegrunnelseFritekst(vilkaarsresultat.getBegrunnelseFritekst());
            vilkaarDtoListe.add(vilkaarDto);
        }

        return vilkaarDtoListe;
    }

    @Transactional(readOnly = true)
    public Optional<Vilkaarsresultat> finnVilkaarsresultat(long behandlingID, Vilkaar vilkaar) {
        return vilkaarsresultatRepo.findByBehandlingsresultatIdAndVilkaar(behandlingID, vilkaar);
    }

    @Transactional(readOnly = true)
    public boolean oppfyllerVilkaar(long behandlingID, Vilkaar vilkaar) {
        return vilkaarsresultatRepo.existsByBehandlingsresultatIdAndVilkaarAndOppfyltTrue(behandlingID, vilkaar);
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

    @Transactional(rollbackFor = MelosysException.class)
    public void registrerVilkår(long behandlingID, List<VilkaarDto> vilkaarDtoer) throws FunksjonellException {
        validerVilkår(vilkaarDtoer);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        tømVilkårForBehandlingsresultat(behandlingsresultat);
        vilkaarsresultatRepo.flush();

        for (VilkaarDto vilkaarDto : vilkaarDtoer) {
            Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultat(behandlingsresultat,
                Vilkaar.valueOf(vilkaarDto.getVilkaar()),
                vilkaarDto.isOppfylt(),
                vilkaarDto.getBegrunnelseKoder(),
                vilkaarDto.getBegrunnelseFritekst());
            vilkaarsresultatRepo.save(vilkaarsresultat);
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void tømVilkårForBehandlingsresultat(Behandlingsresultat behandlingsresultat) {
        vilkaarsresultatRepo.deleteByBehandlingsresultatAndVilkaarNotIn(behandlingsresultat, IMMUTABLE_VILKAAR);
    }

    private void validerVilkår(List<VilkaarDto> vilkaarDtoer) throws FunksjonellException {

        final Collection<String> nyeVilkår = vilkaarDtoer.stream().map(VilkaarDto::getVilkaar).collect(Collectors.toList());

        for (Vilkaar immutableVilkår : IMMUTABLE_VILKAAR) {
            if (nyeVilkår.contains(immutableVilkår.getKode())) {
                throw new FunksjonellException("Kan ikke endre vilkår " + immutableVilkår);
            }
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppdaterVilkaarsresultat(long behandlingID,
                                         Vilkaar vilkaar,
                                         boolean oppfylt,
                                         @Nullable Kodeverk begrunnelseKode) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        vilkaarsresultatRepo.deleteByBehandlingsresultat(behandlingsresultat);
        vilkaarsresultatRepo.flush();
        List<String> begrunnelseKoder = begrunnelseKode == null ? List.of() : List.of(begrunnelseKode.getKode());
        vilkaarsresultatRepo.save(lagVilkaarsresultat(behandlingsresultat, vilkaar, oppfylt, begrunnelseKoder, null));
    }

    private Vilkaarsresultat lagVilkaarsresultat(Behandlingsresultat behandlingsresultat,
                                                 Vilkaar vilkaar,
                                                 boolean oppfylt,
                                                 List<String> begrunnelseKoder,
                                                 String begrunnelseFritekst) {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBehandlingsresultat(behandlingsresultat);
        vilkaarsresultat.setVilkaar(vilkaar);
        vilkaarsresultat.setOppfylt(oppfylt);
        vilkaarsresultat.setBegrunnelser(begrunnelseKoder.stream().map(kode -> lagBegrunnelse(vilkaarsresultat, kode))
            .collect(Collectors.toSet()));
        vilkaarsresultat.setBegrunnelseFritekst(begrunnelseFritekst);
        return vilkaarsresultat;
    }

    private VilkaarBegrunnelse lagBegrunnelse(Vilkaarsresultat vilkaarsresultat, String begrunnelseKode) {
        VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
        begrunnelse.setVilkaarsresultat(vilkaarsresultat);
        begrunnelse.setKode(begrunnelseKode);
        return begrunnelse;
    }
}
