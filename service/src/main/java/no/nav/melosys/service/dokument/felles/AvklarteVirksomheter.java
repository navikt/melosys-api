package no.nav.melosys.service.dokument.felles;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Yrkesaktivitetstyper;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentArbeidsforholdDokument;
import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentSøknadDokument;

public class AvklarteVirksomheter {
    private final RegisterOppslagService registerOppslagService;
    private final Behandling behandling;
    private final Set<String> avklarteOrgnumre;

    public static Function<OrganisasjonDokument, Adresse> ingenAdresse = org -> null;
    public static Function<OrganisasjonDokument, Adresse> ustrukturertForretningsadresse = org -> org.getOrganisasjonDetaljer().hentUstrukturertForretningsadresse();

    public AvklarteVirksomheter(AvklartefaktaService avklartefaktaService,
                                RegisterOppslagService registerOppslagService,
                                Behandling behandling) {
        this.behandling = behandling;
        this.registerOppslagService = registerOppslagService;
        this.avklarteOrgnumre = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
    }

    public Set<String> hentSelvstendigeForetakOrgnumre() throws TekniskException {
        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        Set<String> organisasjonsnumre = søknad.selvstendigArbeid.hentAlleOrganisasjonsnumre()
            .collect(Collectors.toSet());

        organisasjonsnumre.retainAll(avklarteOrgnumre);
        return organisasjonsnumre;
    }

    public Set<String> hentArbeidsgivendeOrgnumre() throws TekniskException {
        ArbeidsforholdDokument arbDok = hentArbeidsforholdDokument(behandling);
        Set<String> arbeidsgivendeOrgnumre = arbDok.hentOrgnumre();
        SoeknadDokument søknad = hentSøknadDokument(behandling);
        arbeidsgivendeOrgnumre.addAll(søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere);

        arbeidsgivendeOrgnumre.retainAll(avklarteOrgnumre);
        return arbeidsgivendeOrgnumre;
    }

    public List<AvklartVirksomhet> hentSelvstendigeForetak(Function<OrganisasjonDokument, Adresse> adressekonverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Set<String> selvstendigeForetakOrgnumre = hentSelvstendigeForetakOrgnumre();
        return registerOppslagService.hentOrganisasjoner(selvstendigeForetakOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.SELVSTENDIG))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentArbeidsgivere(Function<OrganisasjonDokument, Adresse> adressekonverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Set<String> arbeidsgivendeOrgnumre = hentArbeidsgivendeOrgnumre();
        return registerOppslagService.hentOrganisasjoner(arbeidsgivendeOrgnumre).stream()
            .map(org -> new AvklartVirksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), adressekonverterer.apply(org), Yrkesaktivitetstyper.LOENNET_ARBEID))
            .collect(Collectors.toList());
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheter(Function<OrganisasjonDokument, Adresse> adressekonverterer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<AvklartVirksomhet> norskeVirksomheter = hentArbeidsgivere(adressekonverterer);
        norskeVirksomheter.addAll(hentSelvstendigeForetak(adressekonverterer));
        return norskeVirksomheter;
    }

    public Set<String> getAvklarteOrgnumre() {
        return avklarteOrgnumre;
    }

    public int antall() {
        return avklarteOrgnumre.size();
    }
}