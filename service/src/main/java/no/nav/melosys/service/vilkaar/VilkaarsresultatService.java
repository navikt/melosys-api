package no.nav.melosys.service.vilkaar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VilkaarsresultatService {

    private final BehandlingsresultatRepository behandlingsresultatRepo;

    private final VilkaarsresultatRepository vilkaarsresultatRepo;

    @Autowired
    public VilkaarsresultatService(BehandlingsresultatRepository behandlingsresultatRepo, VilkaarsresultatRepository vilkaarsresultatRepo) {
        this.behandlingsresultatRepo = behandlingsresultatRepo;
        this.vilkaarsresultatRepo = vilkaarsresultatRepo;
    }

    @Transactional
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

    @Transactional
    public void registrerVilkår(long behandlingID, List<VilkaarDto> vilkaarDtoer) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepo.findById(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Registrering av vilkår feilet fordi behandlingsresulat med ID " + behandlingID + " er ikke funnet."));

        vilkaarsresultatRepo.deleteByBehandlingsresultat(behandlingsresultat);

        for (VilkaarDto vilkaarDto :  vilkaarDtoer) {
            Vilkaarsresultat vilkaarsresultat = lagNyVilkaarResultat(behandlingsresultat, vilkaarDto);
            vilkaarsresultatRepo.save(vilkaarsresultat);
        }
    }

    private Vilkaarsresultat lagNyVilkaarResultat(Behandlingsresultat behandlingsresultat, VilkaarDto vilkaarDto) throws IkkeFunnetException {

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
        vilkaarsresultat.setVilkaar(Vilkaar.forKode(vilkaarDto.getVilkaar()));
        vilkaarsresultat.setOppfylt(vilkaarDto.isOppfylt());
        return vilkaarsresultat;
    }
}
