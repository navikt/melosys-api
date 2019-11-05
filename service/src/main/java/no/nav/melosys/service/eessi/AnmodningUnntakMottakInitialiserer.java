package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//A001
@Service
public class AnmodningUnntakMottakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private final FagsakService fagsakService;

    @Autowired
    public AnmodningUnntakMottakInitialiserer(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) {
        if (gsakSaksnummer == null) {
            return RutingResultat.NY_SAK;
        }

        // TODO: Avklares hva som skal gjøres ved oppdatert SED
        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().getSistOppdaterteBehandling();
            prosessinstans.setBehandling(behandling);
            if (fagsak.get().getStatus() != Saksstatuser.OPPRETTET) {
                return RutingResultat.INGEN_BEHANDLING;
            }

            return RutingResultat.NY_BEHANDLING;
        }

        return RutingResultat.NY_SAK;
    }

    @Override
    public boolean gjelderSedType(SedType sedType, Landkoder lovvalgsland) {
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
