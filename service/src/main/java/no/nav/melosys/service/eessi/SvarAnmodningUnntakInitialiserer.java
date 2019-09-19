package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//A002,A011
@Service
public class SvarAnmodningUnntakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private final FagsakService fagsakService;
    private final AnmodningsperiodeService anmodningsperiodeService;

    @Autowired
    public SvarAnmodningUnntakInitialiserer(FagsakService fagsakService, AnmodningsperiodeService anmodningsperiodeService) {
        this.fagsakService = fagsakService;
        this.anmodningsperiodeService = anmodningsperiodeService;
    }

    @Override
    @Transactional
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws TekniskException, FunksjonellException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Behandling behandling = hentBehandling(gsakSaksnummer);
        Optional<Anmodningsperiode> anmodningsperiode = anmodningsperiodeService.hentAnmodningsperioder(behandling.getId())
            .stream().findFirst();

        if (!anmodningsperiode.isPresent()) {
            throw new FunksjonellException(String.format(
                "Mottatt SED %s på buctype %s - finner behandling %s for rinasak %s, men behandlingen har ingen anmodningsperiode",
                melosysEessiMelding.getSedType(), melosysEessiMelding.getBucType(), behandling.getId(), melosysEessiMelding.getRinaSaksnummer()));
        }

        prosessinstans.setBehandling(behandling);
        return anmodningsperiode.map(Anmodningsperiode::getAnmodningsperiodeSvar).isPresent() ?
            RutingResultat.INGEN_BEHANDLING : RutingResultat.OPPDATER_BEHANDLING;
    }

    private Behandling hentBehandling(Long gsakSaksnummer) throws TekniskException {
        Fagsak fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)
            .orElseThrow(() -> new TekniskException("Finner ikke fagsak fra gsakSaksnummer " + gsakSaksnummer));

        return fagsak.getAktivBehandling();
    }

    @Override
    public boolean gjelderSedType(SedType sedType, Landkoder lovvalgsland) {
        return sedType == SedType.A011
            || sedType == SedType.A002;
    }

    @Override
    public ProsessType hentAktuellProsessType() {
        return ProsessType.ANMODNING_OM_UNNTAK_SVAR;
    }
}
