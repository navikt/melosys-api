package no.nav.melosys.saksflyt.steg.sed;

import java.util.Collection;
import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.eessi.BehandleMottattSedInitialiserer;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SedMottakRuting extends AbstraktStegBehandler {

    private final Collection<BehandleMottattSedInitialiserer> sedMottattInitialiserere;

    @Autowired
    public SedMottakRuting(Collection<BehandleMottattSedInitialiserer> sedMottattInitialiserere) {
        this.sedMottattInitialiserere = sedMottattInitialiserere;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_RUTING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        BehandleMottattSedInitialiserer behandleMottattSedInitialiserer = hentInitialisererForSedType(SedType.valueOf(melosysEessiMelding.getSedType()));
        behandleMottattSedInitialiserer.initialiserProsessinstans(prosessinstans);

        if (prosessinstans.getSteg() == inngangsSteg()) {
            throw new TekniskException("Prosessinstans ikke oppdatert med nytt steg!");
        }
    }

    private BehandleMottattSedInitialiserer hentInitialisererForSedType(SedType sedType) throws IkkeFunnetException {
        return sedMottattInitialiserere.stream()
            .filter(initialiserer -> initialiserer.gjelderSedType(sedType)).findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Melosys støtter ikke behandling av sedtype" + sedType));
    }
}
