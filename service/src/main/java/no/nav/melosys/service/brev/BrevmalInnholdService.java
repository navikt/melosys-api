package no.nav.melosys.service.brev;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
public class BrevmalInnholdService {

    private final BehandlingService behandlingService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;

    @Autowired
    public BrevmalInnholdService(BehandlingService behandlingService, AvklarteVirksomheterService avklarteVirksomheterService, KodeverkService kodeverkService) {
        this.behandlingService = behandlingService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
    }

    public List<Produserbaredokumenter> hentBrevMaler(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Produserbaredokumenter> brevmaler = asList(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER);

        return behandling.erAktiv() ? brevmaler : emptyList();
    }

    public List<AvklartVirksomhet> hentArbeidsgivere(long behandlingId) throws IkkeFunnetException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        return avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
    }

    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        if (adresse == null || isEmpty(adresse.postnummer)) {
            adresse = org.getOrganisasjonDetaljer().hentStrukturertPostadresse();
        }
        if (isEmpty(adresse.gatenavn)) {
            adresse.gatenavn = " ";
        }
        if (adresse.erNorsk()) {
            adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        } else if (isEmpty(adresse.postnummer)) {
            //Utenlandske adresser har ikke alltid postnummer
            adresse.postnummer = " ";
        }
        return adresse;
    }
}
