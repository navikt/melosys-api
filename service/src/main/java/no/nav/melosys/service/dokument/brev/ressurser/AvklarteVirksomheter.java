package no.nav.melosys.service.dokument.brev.ressurser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class AvklarteVirksomheter {
    private final Behandling behandling;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;

    // Microcachede verdier som kun eksisterer under brevbygging.
    // For å slippe å gjøre register- og kodeverksoppslag gjentatte ganger
    private List<AvklartVirksomhet> norskeVirksomheter;
    private List<AvklartVirksomhet> norskeArbeidsgivere;
    private List<AvklartVirksomhet> norskeSelvstendige;
    private List<AvklartVirksomhet> utenlandskeVirksomheter;

    public AvklarteVirksomheter(Behandling behandling,
                                AvklarteVirksomheterService avklarteVirksomheterService,
                                KodeverkService kodeverkService) {
        this.behandling = behandling;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
    }

    public List<AvklartVirksomhet> hentAlleNorskeVirksomheterMedAdresse() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        if (norskeVirksomheter == null) {
            norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
        }
        return norskeVirksomheter;
    }

    public List<AvklartVirksomhet> hentNorskeArbeidsgivere() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        if (norskeArbeidsgivere == null) {
            norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling, this::utfyllManglendeAdressefelter);
        }
        return norskeArbeidsgivere;
    }

    public List<AvklartVirksomhet> hentNorskeSelvstendige() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        if (norskeSelvstendige == null) {
            norskeSelvstendige = avklarteVirksomheterService.hentNorskeSelvstendigeForetak(behandling, this::utfyllManglendeAdressefelter);
        }
        return norskeSelvstendige;
    }

    public List<AvklartVirksomhet> hentUtenlandskeVirksomheter() throws TekniskException {
        if (utenlandskeVirksomheter == null) {
            utenlandskeVirksomheter = avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling);
        }
        return utenlandskeVirksomheter;
    }

    public Set<String> hentNorskeArbeidsgivendeOrgnumre() throws TekniskException {
        return avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling);
    }

    public Set<String> hentNorskeSelvstendigeForetakOrgnumre() throws TekniskException {
        return avklarteVirksomheterService.hentNorskeSelvstendigeForetakOrgnumre(behandling);
    }

    public AvklartVirksomhet hentHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        if (!hentAlleNorskeVirksomheterMedAdresse().isEmpty()) {
            return hentAlleNorskeVirksomheterMedAdresse().iterator().next();
        } else {
            return hentUtenlandskeVirksomheter().iterator().next();
        }
    }

    public Collection<AvklartVirksomhet> hentBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Collection<AvklartVirksomhet> bivirksomheter = new ArrayList<>();
        bivirksomheter.addAll(hentAlleNorskeVirksomheterMedAdresse());
        bivirksomheter.addAll(hentUtenlandskeVirksomheter());
        bivirksomheter.remove(hentHovedvirksomhet());
        return bivirksomheter;
    }

    private StrukturertAdresse utfyllManglendeAdressefelter(OrganisasjonDokument org) {
        StrukturertAdresse adresse = org.getOrganisasjonDetaljer().hentStrukturertForretningsadresse();
        if (StringUtils.isEmpty(adresse.gatenavn) || StringUtils.isEmpty(adresse.postnummer)) {
            adresse = org.getOrganisasjonDetaljer().hentStrukturertPostadresse();
        }
        adresse.poststed = kodeverkService.dekod(FellesKodeverk.POSTNUMMER, adresse.postnummer, LocalDate.now());
        return adresse;
    }
}
