package no.nav.melosys.saksflyt.agent.unntakmed;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValiderUnntakOmMedlemskap extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ValiderUnntakOmMedlemskap.class);

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.VALIDER_UNNTAK;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        //TODO: validering
        //if validering feilet, opprett oppgave
        prosessinstans.setSteg(ProsessSteg.GSAK_OPPRETT_OPPGAVE);
    }
}
