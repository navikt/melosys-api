package no.nav.melosys.service.dokument;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalResolver;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokgenMalResolver dokgenMalResolver;
    private final JoarkFasade joarkFasade;
    private final DokgenMalMapper dokgenMalMapper;
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer, DokgenMalResolver dokgenMalResolver, JoarkFasade joarkFasade,
                         DokgenMalMapper dokgenMalMapper, BehandlingService behandlingService, EregFasade eregFasade,
                         KontaktopplysningService kontaktopplysningService) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokgenMalResolver = dokgenMalResolver;
        this.joarkFasade = joarkFasade;
        this.dokgenMalMapper = dokgenMalMapper;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartdokument, long behandlingId,
                               Aktoer mottaker) throws FunksjonellException, TekniskException {
        return produserBrev(produserbartdokument, behandlingId, mottaker, false);
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartdokument, long behandlingId,
                               Aktoer mottaker, boolean bestillKopi) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        String malnavn = dokgenMalResolver.hentMalnavn(produserbartdokument);

        DokgenBrevbestilling.Builder brevbestilling = new DokgenBrevbestilling.Builder()
            .medProduserbartdokument(produserbartdokument)
            .medBehandling(behandling);

        if (mottaker.erOrganisasjon()) {
            settOrganisasjonsOpplysninger(behandling, mottaker, brevbestilling);
        }

        settJournalpostOpplysninger(behandling, brevbestilling);

        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(brevbestilling.build());

        return dokgenConsumer.lagPdf(malnavn, dokgenDto, bestillKopi);
    }

    public String hentMalnavn(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        return dokgenMalResolver.hentMalnavn(produserbartDokument);
    }

    boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokgenMalResolver.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private void settOrganisasjonsOpplysninger(Behandling behandling, Aktoer mottaker,
                                               DokgenBrevbestilling.Builder brevbestilling)
        throws IkkeFunnetException, IntegrasjonException {
        brevbestilling
            .medOrg((OrganisasjonDokument) eregFasade.hentOrganisasjon(mottaker.getOrgnr()).getDokument())
            .medKontaktopplysning(
                kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null)
            );
    }

    private void settJournalpostOpplysninger(Behandling behandling, DokgenBrevbestilling.Builder brevbestilling)
        throws SikkerhetsbegrensningException, IntegrasjonException {
        Journalpost journalpost = joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId());
        brevbestilling
            .medForsendelseMottatt(journalpost.getForsendelseMottatt())
            .medAvsenderNavn(journalpost.getAvsenderNavn())
            .medAvsenderId(journalpost.getAvsenderId());
    }
}
