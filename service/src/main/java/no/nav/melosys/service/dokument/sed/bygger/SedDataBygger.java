package no.nav.melosys.service.dokument.sed.bygger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.dokument.sed.SedData;
import no.nav.melosys.service.kodeverk.KodeverkService;

public abstract class SedDataBygger extends AbstraktDokumentDataBygger {

    private RegisterOppslagService registerOppslagService;

    protected SedDataBygger(KodeverkService kodeverkService, RegisterOppslagService registerOppslagService,
                            LovvalgsperiodeService lovvalgsperiodeService, AvklartefaktaService avklartefaktaService) {
        super(kodeverkService, lovvalgsperiodeService, avklartefaktaService);
        this.registerOppslagService = registerOppslagService;

    }

    public <T extends SedData> T lag(Behandling behandling, T sedData)
        throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);
        this.avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());

        sedData.setPersonDokument(this.person);
        sedData.setSøknadDokument(this.søknad);

        sedData.setBostedsadresse(hentBostedsadresse());
        sedData.setArbeidsgivendeVirkomsheter(hentAlleNorskeAvklarteVirksomheter());
        sedData.setArbeidssteder(hentArbeidssteder());
        sedData.setUtenlandskeVirksomheter(hentUtenlandskeVirksomheter()); //Lev1, kun en utenlandsk virksomhet

        if (søknad.selvstendigArbeid.erSelvstendig) {
            sedData.setSelvstendigeVirksomheter(hentAvklarteSelvstendigeForetak());
        }

        return sedData;
    }

    protected List<Virksomhet> hentAlleNorskeAvklarteVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        return registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse()))
            .collect(Collectors.toList());
    }

    protected List<Virksomhet> hentAvklarteSelvstendigeForetak() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<String> organisasjonsnumre = hentAvklarteSelvstendigeForetakOrgnumre();
        return registerOppslagService.hentOrganisasjoner(organisasjonsnumre).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse()))
            .collect(Collectors.toList());
    }
}
