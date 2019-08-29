package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.eessi.BehandleMottattSedInitialiserer;
import no.nav.melosys.service.eessi.InitialiseringResultat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
 * Transisjoner:
 * SED_MOTTAK_RUTING → SED_MOTTAK_FERDIGSTILL_JOURNALPOST
 * eller
 * SED_MOTTAK_RUTING → SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH om sak ikke finnes
 * eller
 * SED_MOTTAK_RUTING -> SED_MOTTAK_OPPRETT_NY_BEHANDLING
 */
@Component
public class SedMottakRuting extends AbstraktStegBehandler {

    private final Collection<BehandleMottattSedInitialiserer> sedMottattInitialiserere;
    private final EessiService eessiService;

    @Autowired
    public SedMottakRuting(Collection<BehandleMottattSedInitialiserer> sedMottattInitialiserere, EessiService eessiService) {
        this.sedMottattInitialiserere = sedMottattInitialiserere;
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_RUTING;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        Optional<Long> gsakSaksnummer = eessiService.hentSakForRinasaksnummer(melosysEessiMelding.getRinaSaksnummer());
        gsakSaksnummer.ifPresent(g -> prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, g));

        BehandleMottattSedInitialiserer behandleMottattSedInitialiserer = hentInitialisererForSedType(SedType.valueOf(melosysEessiMelding.getSedType()));
        InitialiseringResultat resultat = behandleMottattSedInitialiserer
            .initialiserProsessinstans(prosessinstans, gsakSaksnummer.orElse(null));

        if (resultat == InitialiseringResultat.INGEN_BEHANDLING || resultat == InitialiseringResultat.OPPDATER_BEHANDLING) {
            validerBehandlingErSatt(prosessinstans);
            prosessinstans.setType(ProsessType.MOTTAK_SED_JOURNALFØRING);
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);

        } else if (resultat == InitialiseringResultat.NY_BEHANDLING) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandleMottattSedInitialiserer.hentBehandlingstype(melosysEessiMelding));
            prosessinstans.setType(behandleMottattSedInitialiserer.hentAktuellProsessType());
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_NY_BEHANDLING);

        } else if (resultat == InitialiseringResultat.NY_SAK) {
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandleMottattSedInitialiserer.hentBehandlingstype(melosysEessiMelding));
            prosessinstans.setType(behandleMottattSedInitialiserer.hentAktuellProsessType());
            prosessinstans.setSteg(ProsessSteg.SED_MOTTAK_OPPRETT_SAK_OG_BEH);

        } else {
            throw new TekniskException("Ukjent Initialiseringsresultat: " + resultat);
        }
    }

    private void validerBehandlingErSatt(Prosessinstans prosessinstans) throws TekniskException {
        if (prosessinstans.getBehandling() == null) {
            throw new TekniskException("Prosessinstansen må ha en fagsak knyttet til seg for å kunne journalføre SED!");
        }
    }

    private BehandleMottattSedInitialiserer hentInitialisererForSedType(SedType sedType) throws IkkeFunnetException {
        return sedMottattInitialiserere.stream()
            .filter(initialiserer -> initialiserer.gjelderSedType(sedType)).findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Melosys støtter ikke behandling av sedtype" + sedType));
    }
}
