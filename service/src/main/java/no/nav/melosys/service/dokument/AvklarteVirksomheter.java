package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class AvklarteVirksomheter {
    private RegisterOppslagService registerOppslagService;
    private Behandling behandling;
    private Set<String> avklarteOrgnumre;

    public AvklarteVirksomheter(AvklartefaktaService avklartefaktaService,
                                RegisterOppslagService registerOppslagService,
                                Behandling behandling) throws TekniskException {
        this.behandling = behandling;
        this.registerOppslagService = registerOppslagService;
        this.avklarteOrgnumre = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
    }

    public void avklarSelvstendigForetakVirksomhet(Virksomhet hovedvirksomhet) throws TekniskException {
        Set<String> avklarteSelvstendigeForetakOrgnumre = hentAvklarteSelvstendigeForetakOrgnumre();

        boolean erSelvstendigForetak = avklarteSelvstendigeForetakOrgnumre.stream()
            .anyMatch(orgnummer -> orgnummer.equalsIgnoreCase(hovedvirksomhet.orgnr));

        hovedvirksomhet.setSelvstendigForetak(erSelvstendigForetak);
    }

    public Set<String> hentAvklarteSelvstendigeForetakOrgnumre() throws TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
            .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrgnumre);
        return organisasjonsnumre;
    }

    public List<Virksomhet> hentAvklarteSelvstendigeForetak(Function<OrganisasjonDokument, Adresse> adressekoverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Set<String> organisasjonsnumre = hentAvklarteSelvstendigeForetakOrgnumre();
        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekoverterer.apply(org)))
            .collect(Collectors.toList());
    }

    public List<Virksomhet> hentAlleNorskeAvklarteVirksomheter(Function<OrganisasjonDokument, Adresse> adressekoverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        return registerOppslagService.hentOrganisasjoner(avklarteOrgnumre).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekoverterer.apply(org)))
            .collect(Collectors.toList());
    }

    public Set<String> getAvklarteOrgnumre() {
        return avklarteOrgnumre;
    }

    public int antall() {
        return avklarteOrgnumre.size();
    }
}