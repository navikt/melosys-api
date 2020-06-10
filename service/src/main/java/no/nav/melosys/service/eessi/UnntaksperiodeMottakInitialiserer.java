package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//A009,A010
@Service
public class UnntaksperiodeMottakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeMottakInitialiserer.class);

    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public UnntaksperiodeMottakInitialiserer(FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws FunksjonellException, TekniskException {

        if (gsakSaksnummer == null) {
            return RutingResultat.NY_SAK;
        }

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().hentSistAktiveBehandling();
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            if (periodeErEndret(melosysEessiMelding, behandlingsresultat)) {
                log.info("Mottatt ny {} i {} hvor periode er endret. Oppretter ny behandling",
                    melosysEessiMelding.getSedType(), fagsak.get().getSaksnummer());
                return RutingResultat.NY_BEHANDLING;
            } else {
                log.info("Mottatt ny {} i {} periode er ikke endret. Oppretter ikke ny behandling",
                    melosysEessiMelding.getSedType(), fagsak.get().getSaksnummer());
                prosessinstans.setBehandling(behandling);
                return RutingResultat.INGEN_BEHANDLING;
            }
        } else {
            return RutingResultat.NY_SAK;
        }
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A009
            || sedType == SedType.A010;
    }

    @Override
    public Behandlingstema hentBehandlingstema(MelosysEessiMelding melosysEessiMelding) {
        return hentBehandlingstemaForSedType(SedType.valueOf(melosysEessiMelding.getSedType()));
    }

    @Override
    public ProsessType hentAktuellProsessType() {
        return ProsessType.REGISTRERING_UNNTAK;
    }

    private Behandlingstema hentBehandlingstemaForSedType(SedType sedType) {
        if (sedType == SedType.A009) {
            return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        } else if (sedType == SedType.A010) {
            return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE;
        } else if (sedType == SedType.A003) {
            return Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
        }

        throw new IllegalArgumentException("UnntaksperiodeMottakInitialiserer støtter ikke sedtype " + sedType);
    }
}
