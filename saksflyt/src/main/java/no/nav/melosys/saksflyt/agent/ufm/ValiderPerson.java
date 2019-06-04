package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValiderPerson extends RegistreringUnntakValiderer {

    private static final Logger log = LoggerFactory.getLogger(ValiderPerson.class);

    @Autowired
    ValiderPerson(AvklartefaktaService avklartefaktaService) {
        super(avklartefaktaService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_VALIDER_PERSON;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(prosessinstans.getBehandling());

        LocalDate dødsdato = personDokument.dødsdato;

        if (dødsdato != null) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.PERSON_DOD);
        }
    }
}
