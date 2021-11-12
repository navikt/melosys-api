package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static org.springframework.util.StringUtils.hasText;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper;
    private final JoarkFasade joarkFasade;
    private final DokgenMalMapper dokgenMalMapper;
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final BrevmottakerService brevmottakerService;
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlerService saksbehandlerService;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer,
                         DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper,
                         @Qualifier("system") JoarkFasade joarkFasade,
                         DokgenMalMapper dokgenMalMapper, BehandlingService behandlingService,
                         @Qualifier("system") EregFasade eregFasade,
                         KontaktopplysningService kontaktopplysningService,
                         BrevmottakerService brevmottakerService, ProsessinstansService prosessinstansService,
                         SaksbehandlerService saksbehandlerService) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokumentproduksjonsInfoMapper = dokumentproduksjonsInfoMapper;
        this.joarkFasade = joarkFasade;
        this.dokgenMalMapper = dokgenMalMapper;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.brevmottakerService = brevmottakerService;
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlerService = saksbehandlerService;
    }

    public byte[] produserUtkast(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        Produserbaredokumenter produserbartdokument = brevbestillingRequest.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Aktoer mottaker;
        if (hasText(brevbestillingRequest.getOrgNr())) {
            mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingRequest.getMottaker());
            mottaker.setOrgnr(brevbestillingRequest.getOrgNr());
        } else {
            mottaker = brevmottakerService.avklarMottakere(produserbartdokument,
                Mottaker.av(brevbestillingRequest.getMottaker()), behandling, true, false).get(0);
        }

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingRequest);

        brevbestilling
            .medProduserbartdokument(produserbartdokument)
            .medBehandlingId(behandlingId)
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingRequest.getBestillersId()))
            .medBestillUtkast(true);

        return produserBrev(mottaker, brevbestilling.build());
    }

    public byte[] produserBrev(Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        Behandling behandling = behandlingService.hentBehandling(brevbestilling.getBehandlingId());
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(brevbestilling.getProduserbartdokument());
        String orgnr = mottaker != null ? mottaker.getOrgnr() : null;
        DokgenBrevbestilling.Builder<?> builder = brevbestilling.toBuilder();

        builder.medBehandling(behandling);

        if (hasText(orgnr)) {
            settOrganisasjonsOpplysninger(behandling, orgnr, builder);
        }

        settJournalpostOpplysninger(behandling, builder);

        var dokgenDto = dokgenMalMapper.mapBehandling(builder.build());

        return dokgenConsumer.lagPdf(malnavn, dokgenDto, brevbestilling.isBestillKopi(), brevbestilling.isBestillUtkast());
    }

    public void produserOgDistribuerBrev(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        Produserbaredokumenter produserbartDokument = brevbestillingRequest.getProduserbardokument();
        var behandling = behandlingService.hentBehandling(behandlingId);

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingRequest);

        brevbestilling
            .medProduserbartdokument(produserbartDokument)
            .medBehandlingId(behandlingId)
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingRequest.getBestillersId()));

        List<Aktoer> mottakere = new ArrayList<>();
        if (hasText(brevbestillingRequest.getOrgNr())) {
            Aktoer mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingRequest.getMottaker());
            mottaker.setOrgnr(brevbestillingRequest.getOrgNr());
            mottakere.add(mottaker);
        } else {
            mottakere = brevmottakerService.avklarMottakere(produserbartDokument,
                Mottaker.av(brevbestillingRequest.getMottaker()), behandling, false, false);
        }

        for (Aktoer aktoer : mottakere) {
            produserOgDistribuerBrev(behandling, aktoer, brevbestilling.build());
        }

        for (KopiMottaker kopiMottaker : brevbestillingRequest.getKopiMottakere()) {
            var aktoer = new Aktoer();
            aktoer.setRolle(kopiMottaker.getRolle());
            aktoer.setOrgnr(kopiMottaker.getOrgnr());
            aktoer.setAktørId(kopiMottaker.getAktørId());
            brevbestilling.medBestillKopi(true);
            produserOgDistribuerBrev(behandling, aktoer, brevbestilling.build());
        }
    }

    private void produserOgDistribuerBrev(Behandling behandling, Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);
    }

    public DokumentproduksjonsInfo hentDokumentInfo(Produserbaredokumenter produserbartDokument) {
        return dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(produserbartDokument);
    }

    public boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokumentproduksjonsInfoMapper.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private void settOrganisasjonsOpplysninger(Behandling behandling, String orgnr,
                                               DokgenBrevbestilling.Builder<?> brevbestilling) {
        var kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        brevbestilling
            .medOrg((OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument())
            .medKontaktopplysning(kontaktopplysning);
    }

    private void settJournalpostOpplysninger(Behandling behandling, DokgenBrevbestilling.Builder<?> brevbestilling) {
        var journalpost = joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId());
        brevbestilling
            .medForsendelseMottatt(journalpost.getForsendelseMottatt())
            .medAvsenderNavn(journalpost.getAvsenderNavn())
            .medAvsendertype(journalpost.getAvsenderType())
            .medAvsenderLand(journalpost.getAvsenderLand());
    }

    private String hentSaksbehandlerNavn(String ident) {
        return ident != null ? saksbehandlerService.hentNavnForIdent(ident) : "N/A";
    }

    private DokgenBrevbestilling.Builder<?> lagDokgenBrevbestilling(BrevbestillingRequest brevbestillingRequest) {
        return switch (brevbestillingRequest.getProduserbardokument()) {
            case MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER -> new MangelbrevBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingRequest.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingRequest.getManglerFritekst())
                .medKontaktpersonNavn(brevbestillingRequest.getKontaktpersonNavn());
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8 -> new InnvilgelseBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingRequest.getInnledningFritekst())
                .medBegrunnelseFritekst(brevbestillingRequest.getBegrunnelseFritekst())
                .medEktefelleFritekst(brevbestillingRequest.getEktefelleFritekst())
                .medBarnFritekst(brevbestillingRequest.getBarnFritekst());
            case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER -> new FritekstbrevBrevbestilling.Builder()
                .medFritekstTittel(brevbestillingRequest.getFritekstTittel())
                .medFritekst(brevbestillingRequest.getFritekst())
                .medKontaktopplysninger(brevbestillingRequest.isKontaktopplysninger());
            case ATTEST_NO_UK_1 -> new AttestStorbritanniaBrevbestilling.Builder();
            default -> new DokgenBrevbestilling.Builder<>();
        };
    }
}
