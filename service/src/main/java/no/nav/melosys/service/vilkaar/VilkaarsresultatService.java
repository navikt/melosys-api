package no.nav.melosys.service.vilkaar;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;

@Service
public class VilkaarsresultatService {

    private final BehandlingsresultatRepository behandlingsresultatRepo;

    private final VilkaarsresultatRepository vilkaarsresultatRepo;

    @Autowired
    public VilkaarsresultatService(BehandlingsresultatRepository behandlingsresultatRepo, VilkaarsresultatRepository vilkaarsresultatRepo) {
        this.behandlingsresultatRepo = behandlingsresultatRepo;
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
    public void registrerVilkår(long behandlingID, List<VilkaarDto> vilkaarDtoer) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Registrering av vilkår feilet fordi behandlingsresulat med ID " + behandlingID + " er ikke funnet."));

        vilkaarsresultatRepo.deleteByBehandlingsresultat(behandlingsresultat);
        vilkaarsresultatRepo.flush();

        for (VilkaarDto vilkaarDto :  vilkaarDtoer) {
            Vilkaarsresultat vilkaarsresultat = lagNyVilkaarResultat(behandlingsresultat, vilkaarDto);
            vilkaarsresultatRepo.save(vilkaarsresultat);
        }
    }

    private Vilkaarsresultat lagNyVilkaarResultat(Behandlingsresultat behandlingsresultat, VilkaarDto vilkaarDto) {

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();

        Set<VilkaarBegrunnelse> nyeBegrunnelser = new HashSet<>();
        for (String kode : vilkaarDto.getBegrunnelseKoder()) {
            VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
            begrunnelse.setVilkaarsresultat(vilkaarsresultat);
            begrunnelse.setKode(kode);
            nyeBegrunnelser.add(begrunnelse);
        }
        vilkaarsresultat.setBegrunnelser(nyeBegrunnelser);

        vilkaarsresultat.setBegrunnelseFritekst(vilkaarDto.getBegrunnelseFritekst());
        vilkaarsresultat.setBehandlingsresultat(behandlingsresultat);
        vilkaarsresultat.setVilkaar(Vilkaar.valueOf(vilkaarDto.getVilkaar()));
        vilkaarsresultat.setOppfylt(vilkaarDto.isOppfylt());
        return vilkaarsresultat;
    }
}
