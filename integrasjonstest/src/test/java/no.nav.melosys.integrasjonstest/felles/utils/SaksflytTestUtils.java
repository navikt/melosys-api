package no.nav.melosys.integrasjonstest.felles.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;
import static org.assertj.core.api.Assertions.assertThat;

public final class SaksflytTestUtils {
    private SaksflytTestUtils() {}

    public static VilkaarDto lagVilkaarDto(Vilkaar vilkaar, boolean oppfylt, Kodeverk... vilkårbegrunnelser) {
        VilkaarDto vilkaarDto = new VilkaarDto();
        vilkaarDto.setOppfylt(oppfylt);
        vilkaarDto.setVilkaar(vilkaar.getKode());
        vilkaarDto.setBegrunnelseKoder(new ArrayList<>());
        for (Kodeverk begrunnelseKode : vilkårbegrunnelser) {
            vilkaarDto.getBegrunnelseKoder().add(begrunnelseKode.getKode());
        }
        return vilkaarDto;
    }

    public static LovvalgsperiodeDto lagLovvalgsperiodeDto(LovvalgBestemmelse lovvalgsbestemmelse, Landkoder landkode, InnvilgelsesResultat innvilgelsesResultat) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(landkode);
        lovvalgsperiode.setBestemmelse(lovvalgsbestemmelse);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        lovvalgsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        if (innvilgelsesResultat == InnvilgelsesResultat.INNVILGET) {
            lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
            lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        }
        return LovvalgsperiodeDto.av(lovvalgsperiode);
    }

    public static AktoerDto lagAktørBrukerDto(String aktoerID) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setRolleKode(BRUKER.getKode());
        aktoerDto.setAktoerID(aktoerID);
        return aktoerDto;
    }

    public static AktoerDto lagAktørRepresentantDto(String orgnr, Representerer representerer) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setRolleKode(REPRESENTANT.getKode());
        aktoerDto.setOrgnr(orgnr);
        aktoerDto.setRepresentererKode(representerer.getKode());
        return aktoerDto;
    }

    public static List<ProsessSteg> hentProsessStegForBehandling(ProsessinstansRepository repository, long behandlingsid) {
        return repository.findAll().stream()
            .filter(pi -> pi.getBehandling().getId() == behandlingsid)
            .map(Prosessinstans::getSteg)
            .collect(Collectors.toList());
    }

    public static void sjekkProsessteg(ProsessinstansRepository repository, long behandlingId, ProsessSteg forventetSteg) {
        List<ProsessSteg> lagredeProsessSteg = hentProsessStegForBehandling(repository, behandlingId);
        assertThat(lagredeProsessSteg).containsExactly(forventetSteg);
    }
}
