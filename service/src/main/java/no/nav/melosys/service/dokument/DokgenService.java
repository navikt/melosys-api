package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.service.dokument.brev.mapper.DokgenMalMapper;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final DokumentHentingService dokumentHentingService;

    public DokgenService(DokgenConsumer dokgenConsumer,
                         DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper,
                         @Qualifier("system") JoarkFasade joarkFasade,
                         DokgenMalMapper dokgenMalMapper, BehandlingService behandlingService,
                         @Qualifier("system") EregFasade eregFasade,
                         KontaktopplysningService kontaktopplysningService,
                         BrevmottakerService brevmottakerService, ProsessinstansService prosessinstansService,
                         SaksbehandlerService saksbehandlerService,
                         UtenlandskMyndighetService utenlandskMyndighetService,
                         DokumentHentingService dokumentHentingService) {
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
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.dokumentHentingService = dokumentHentingService;
    }

    @Transactional
    public byte[] produserUtkast(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        Produserbaredokumenter produserbartdokument = brevbestillingRequest.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Aktoer mottaker;
        if (hasText(brevbestillingRequest.getOrgNr()) || hasText(brevbestillingRequest.getInstitusjonId())) {
            mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingRequest.getMottaker());
            mottaker.setOrgnr(brevbestillingRequest.getOrgNr());
            mottaker.setInstitusjonId(brevbestillingRequest.getInstitusjonId());
        } else {
            mottaker = brevmottakerService.avklarMottakere(produserbartdokument,
                Mottaker.av(brevbestillingRequest.getMottaker()), behandling, true, false).get(0);
        }

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingRequest);

        brevbestilling
            .medProduserbartdokument(produserbartdokument)
            .medBehandlingId(behandlingId)
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingRequest.getBestillersId()))
            .medSaksvedleggBestilling(lagSaksvedleggBestilling(brevbestillingRequest.getSaksVedlegg()))
            .medBestillUtkast(true);

        return produserBrev(mottaker, brevbestilling.build(), true);
    }

    @Transactional
    public byte[] produserBrev(Aktoer mottaker, DokgenBrevbestilling brevbestilling, boolean skalFletteVedlegg) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(brevbestilling.getBehandlingId());
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(brevbestilling.getProduserbartdokument());
        String orgnr = mottaker != null ? mottaker.getOrgnr() : null;
        DokgenBrevbestilling.Builder<?> builder = brevbestilling.toBuilder();

        builder.medBehandling(behandling);

        if (hasText(orgnr)) {
            settOrganisasjonsOpplysninger(behandling, orgnr, builder);
        }
        if (mottaker != null && mottaker.erUtenlandskMyndighet()) {
            settUtenlandskMyndighetOpplysninger(mottaker.hentMyndighetLandkode(), builder);
        }

        settJournalpostOpplysninger(behandling, builder);

        var dokgenDto = dokgenMalMapper.mapBehandling(builder.build());
        if (!CollectionUtils.isEmpty(brevbestilling.getSaksvedleggBestilling()) && skalFletteVedlegg) {
            return dokgenConsumer.lagPdfMedVedlegg(malnavn, dokgenDto, brevbestilling.isBestillKopi(),
                brevbestilling.isBestillUtkast(), hentVedleggDokumenterFraJoark(brevbestilling.getSaksvedleggBestilling()));
        }
        return dokgenConsumer.lagPdf(malnavn, dokgenDto, brevbestilling.isBestillKopi(), brevbestilling.isBestillUtkast());
    }

    @Transactional
    public void produserOgDistribuerBrev(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        Produserbaredokumenter produserbartDokument = brevbestillingRequest.getProduserbardokument();
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingRequest);

        brevbestilling
            .medProduserbartdokument(produserbartDokument)
            .medBehandlingId(behandlingId)
            .medSaksvedleggBestilling(lagSaksvedleggBestilling(brevbestillingRequest.getSaksVedlegg()))
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
            aktoer.setRolle(kopiMottaker.rolle());
            aktoer.setOrgnr(kopiMottaker.orgnr());
            aktoer.setAktørId(kopiMottaker.aktørId());
            aktoer.setInstitusjonId(kopiMottaker.institusjonId());
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

    private void settUtenlandskMyndighetOpplysninger(Landkoder landkode,
                                                     DokgenBrevbestilling.Builder<?> brevbestilling) {
        var utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(landkode);
        brevbestilling.medUtenlandskMyndighet(utenlandskMyndighet);
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

    private boolean inneholderArbeidsgiverSomKopimottaker(Collection<KopiMottaker> kopimottakere) {
        return kopimottakere.stream().map(KopiMottaker::rolle).anyMatch(kopimottaker -> kopimottaker == Aktoersroller.ARBEIDSGIVER);
    }

    private List<SaksvedleggBestilling> lagSaksvedleggBestilling(List<SaksvedleggDto> saksvedleggDtoer) {
        if (saksvedleggDtoer == null) {
            return null;
        }

        return saksvedleggDtoer.stream()
            .map(saksvedlegg -> new SaksvedleggBestilling(saksvedlegg.journalpostID(), saksvedlegg.dokumentID()))
            .toList();
    }

    private List<byte[]> hentVedleggDokumenterFraJoark(List<SaksvedleggBestilling> saksvedleggBestillingListe) {
        if (saksvedleggBestillingListe == null) {
            return null;
        }
        return saksvedleggBestillingListe.stream()
            .map(vedleggBestilling ->
                dokumentHentingService.hentDokument(vedleggBestilling.journalpostID(), vedleggBestilling.dokumentID()))
            .toList();
    }

    private DokgenBrevbestilling.Builder<?> lagDokgenBrevbestilling(BrevbestillingRequest brevbestillingRequest) {
        return switch (brevbestillingRequest.getProduserbardokument()) {
            case MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER -> new MangelbrevBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingRequest.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingRequest.getManglerFritekst())
                .medKontaktpersonNavn(brevbestillingRequest.getKontaktpersonNavn());
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8, STORBRITANNIA -> new InnvilgelseBrevbestilling.Builder()
                .medInnledningFritekst(brevbestillingRequest.getInnledningFritekst())
                .medBegrunnelseFritekst(brevbestillingRequest.getBegrunnelseFritekst())
                .medEktefelleFritekst(brevbestillingRequest.getEktefelleFritekst())
                .medBarnFritekst(brevbestillingRequest.getBarnFritekst())
                .medVirksomhetArbeidsgiverSkalHaKopi(inneholderArbeidsgiverSomKopimottaker(brevbestillingRequest.getKopiMottakere()))
                .medNyVurderingBakgrunn(brevbestillingRequest.getNyVurderingBakgrunn());
            case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER ->
                new FritekstbrevBrevbestilling.Builder()
                    .medFritekstTittel(brevbestillingRequest.getFritekstTittel())
                    .medFritekst(brevbestillingRequest.getFritekst())
                    .medKontaktpersonNavn(brevbestillingRequest.getKontaktpersonNavn())
                    .medKontaktopplysninger(brevbestillingRequest.isKontaktopplysninger());
            case AVSLAG_MANGLENDE_OPPLYSNINGER -> new AvslagBrevbestilling.Builder()
                .medFritekst(brevbestillingRequest.getFritekst());
            default -> new DokgenBrevbestilling.Builder<>();
        };
    }
}
