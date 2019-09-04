package no.nav.melosys.service.eessi;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//A002,A011
@Service
public class SvarAnmodningUnntakInitialiserer implements BehandleMottattSedInitialiserer {

    private final FagsakService fagsakService;

    @Autowired
    public SvarAnmodningUnntakInitialiserer(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    @Transactional
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws TekniskException, FunksjonellException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Behandling behandling = hentBehandling(gsakSaksnummer);

        if (behandling.getStatus() != Behandlingsstatus.ANMODNING_UNNTAK_SENDT) {
            throw new FunksjonellException("Finner behandling " + behandling.getId() + " for sed fra rinaSak " + melosysEessiMelding.getRinaSaksnummer()
                + ", men behandlingen har status " + behandling.getStatus());
        }

        prosessinstans.setBehandling(behandling);
        return RutingResultat.OPPDATER_BEHANDLING;
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
