package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;
import java.util.GregorianCalendar;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.Bruker;
import no.nav.melosys.domain.Kjoenn;
import no.nav.melosys.integrasjon.felles.IntegrasjonException;
import no.nav.melosys.integrasjon.tps.aktoer.AktorConsumer;
import no.nav.melosys.integrasjon.tps.person.PersonConsumer;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.person.v2.binding.HentKjerneinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v2.binding.HentKjerneinformasjonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Foedselsdato;
import no.nav.tjeneste.virksomhet.person.v2.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v2.meldinger.HentKjerneinformasjonRequest;
import no.nav.tjeneste.virksomhet.person.v2.meldinger.HentKjerneinformasjonResponse;

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
    public Bruker hentKjerneinformasjon(Bruker bruker) {
        if (bruker.getFnr() == null) {
            throw new IllegalArgumentException("Fnr er ikke satt");
        }

        Person p = hentKjerneinformasjon(bruker.getFnr());

        String navn = p.getPersonnavn().getSammensattNavn();
        bruker.setNavn(navn);

        bruker.setFødselsdato(xmlTilLocalDate(p.getFoedselsdato()));

        String kjønnKode = p.getKjoenn().getKjoenn().getValue();
        Kjoenn kjønn = Kjoenn.getFraKode(kjønnKode);
        bruker.setKjønn(kjønn);

        bruker.setDiskresjonskode(p.getDiskresjonskode().getValue());

        return bruker;

    }

    private LocalDate xmlTilLocalDate(Foedselsdato fødselsdatoXml) {
        LocalDate fødselsdato = null;
        if (fødselsdatoXml != null) {
            GregorianCalendar cal = fødselsdatoXml.getFoedselsdato().toGregorianCalendar();
            fødselsdato = cal.toZonedDateTime().toLocalDate();
        }
        return fødselsdato;
    }

    private Person hentKjerneinformasjon(String fnr) {
        HentKjerneinformasjonRequest request = new HentKjerneinformasjonRequest();
        request.setIdent(fnr);
        try {
            HentKjerneinformasjonResponse response = personConsumer.hentKjerneinformasjon(request);
            Person person = response.getPerson();
            return person;
        } catch (HentKjerneinformasjonPersonIkkeFunnet e) {
            throw new IntegrasjonException(e);
        } catch (HentKjerneinformasjonSikkerhetsbegrensning e) { // NOSONAR
            throw new IntegrasjonException(e);
        }
    }

}
