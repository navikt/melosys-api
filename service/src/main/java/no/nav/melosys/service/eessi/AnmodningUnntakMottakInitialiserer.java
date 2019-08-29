package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//A001
@Service
public class AnmodningUnntakMottakInitialiserer implements BehandleMottattSedInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakMottakInitialiserer.class);

    private final FagsakService fagsakService;

    @Autowired
    public AnmodningUnntakMottakInitialiserer(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    public InitialiseringResultat initialiserProsessinstans(Prosessinstans prosessinstans, Long gsakSaksnummer) {
        if (gsakSaksnummer == null) {
            return InitialiseringResultat.NY_SAK;
        }

        // TODO: Avklares hva som skal gjøres ved oppdatert SED
        Optional<Fagsak> fagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer);

        if (fagsak.isPresent()) {
            if (fagsak.get().getStatus() != Saksstatuser.OPPRETTET) {
                return InitialiseringResultat.INGEN_BEHANDLING;
            }

            return InitialiseringResultat.NY_BEHANDLING;
        }

        return InitialiseringResultat.NY_SAK;
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A001;
    }

    @Override
    public Behandlingstyper hentBehandlingstype(MelosysEessiMelding melosysEessiMelding) {
        return Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL;
    }

    @Override
    public ProsessType hentAktuellProsessType() {
        return ProsessType.ANMODNING_OM_UNNTAK_MOTTAK;
    }
}
