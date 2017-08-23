package no.nav.melosys.integrasjon.tps;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

@Service
public class TpsService implements TpsFasade {

    private static final Logger log = LoggerFactory.getLogger(TpsService.class);

    private AktorConsumer aktorConsumer;

    private PersonConsumer personConsumer;

    @Autowired
    public TpsService(AktorConsumer aktorConsumer, PersonConsumer personConsumer) {
        this.aktorConsumer = aktorConsumer;
        this.personConsumer = personConsumer;
    }

    @Override
    public Optional<String> hentAktørIdForIdent(String fnr) {
        HentAktoerIdForIdentRequest request = new HentAktoerIdForIdentRequest();
        request.setIdent(fnr);

        Optional<String> optResult = null;
        try {
            HentAktoerIdForIdentResponse response = aktorConsumer.hentAktørIdForIdent(request);
            String aktørId = response.getAktoerId();
            optResult = Optional.of(aktørId);
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) { // NOSONAR
            optResult = Optional.empty();
        }
        return optResult;
    }

    @Override
    public Optional<String> hentIdentForAktørId(String aktørID) {
        HentIdentForAktoerIdRequest request = new HentIdentForAktoerIdRequest();
        request.setAktoerId(aktørID);
        
        Optional<String> optResult = null;
        try {
            HentIdentForAktoerIdResponse response = aktorConsumer.hentIdentForAktoerId(request);
        } catch (HentIdentForAktoerIdPersonIkkeFunnet hentIdentForAktoerIdPersonIkkeFunnet) { // NOSONAR
            optResult = Optional.empty();
        }

        return optResult;
    }

    @Override
    public HentPersonResponse hentPerson(String ident) throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        return hentPerson(ident, null);
    }

    @Override
    public HentPersonResponse hentPerson(String ident, Collection<Informasjonsbehov> behov) throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        HentPersonRequest request = new HentPersonRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);
        if (behov != null) {
            request.getInformasjonsbehov().addAll(behov);
        }

        return personConsumer.hentPerson(request);
    }

}
