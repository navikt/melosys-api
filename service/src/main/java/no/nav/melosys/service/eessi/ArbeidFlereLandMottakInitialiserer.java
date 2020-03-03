package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;

//A003
@Service
public class ArbeidFlereLandMottakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private final FagsakService fagsakService;

    public ArbeidFlereLandMottakInitialiserer(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @Override
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws TekniskException, FunksjonellException {

        if (gsakSaksnummer == null) {
            return RutingResultat.NY_SAK;
        }

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer);

        if (fagsak.isEmpty()) {
            throw new FunksjonellException("Finner ingen sak tilknyttet gsaksaksnummer " + gsakSaksnummer);
        }

        //TODO: avklar hva som gjøres ved oppdatert SED
        Behandling behandling = fagsak.get().getAktivBehandling();
        return behandling.erAvsluttet() ? RutingResultat.NY_BEHANDLING : RutingResultat.OPPDATER_BEHANDLING;
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A003;
    }

    @Override
    public Behandlingstyper hentBehandlingstype(MelosysEessiMelding melosysEessiMelding) {
        return Landkoder.NO.getKode().equals(melosysEessiMelding.getLovvalgsland())
            ? Behandlingstyper.UTL_MYND_UTPEKT_NORGE
            : Behandlingstyper.UTL_MYND_UTPEKT_SEG_SELV;
    }

    @Override
    public ProsessType hentAktuellProsessType() {
        return ProsessType.ARBEID_FLERE_LAND;
    }
}
