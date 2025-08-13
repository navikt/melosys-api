# Tracking: Java to Kotlin Test Files Review and Fixes

This file tracks the progress of reviewing and fixing Kotlin test files that were converted from Java.

## Summary
- **Total Kotlin Test Files to Review**: 124 files
- **Files Completed**: 13
- **Files Not Started**: 111

## Test Files to Review

### Frontend-API Module (2 files)

1. **File**: `frontend-api/src/test/kotlin/no/nav/melosys/tjenester/gui/EessiControllerKtTest.kt`
   - **Java Test File**: `frontend-api/src/test/java/no/nav/melosys/tjenester/gui/EessiControllerTest.java`
   - **Main Class File**: `frontend-api/src/main/java/no/nav/melosys/tjenester/gui/EessiController.java`
   - **ProgressFile**: EessiControllerKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

2. **File**: `frontend-api/src/test/kotlin/no/nav/melosys/tjenester/gui/FeatureToggleControllerKtTest.kt`
   - **Java Test File**: `frontend-api/src/test/java/no/nav/melosys/tjenester/gui/FeatureToggleControllerTest.java`
   - **Main Class File**: `frontend-api/src/main/java/no/nav/melosys/tjenester/gui/FeatureToggleController.java`
   - **ProgressFile**: FeatureToggleControllerKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

### Saksflyt Module (2 files)

3. **File**: `saksflyt/src/test/kotlin/no/nav/melosys/saksflyt/steg/brev/BestillBrevKtTest.kt`
   - **Java Test File**: `saksflyt/src/test/java/no/nav/melosys/saksflyt/steg/brev/BestillBrevTest.java`
   - **Main Class File**: `saksflyt/src/main/java/no/nav/melosys/saksflyt/steg/brev/BestillBrev.java`
   - **ProgressFile**: BestillBrevKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

4. **File**: `saksflyt/src/test/kotlin/no/nav/melosys/saksflyt/steg/jfr/OpprettArkivsakKtTest.kt`
   - **Java Test File**: `saksflyt/src/test/java/no/nav/melosys/saksflyt/steg/jfr/OpprettArkivsakTest.java`
   - **Main Class File**: `saksflyt/src/main/java/no/nav/melosys/saksflyt/steg/jfr/OpprettArkivsak.java`
   - **ProgressFile**: OpprettArkivsakKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

### Service Module - Aktør (2 files)

5. **File**: `service/src/test/kotlin/no/nav/melosys/service/aktoer/KontaktopplysningServiceKtTest.kt`
   - **Java Test File**: `service/src/test/java/no/nav/melosys/service/aktoer/KontaktopplysningServiceTest.java`
   - **Main Class File**: `service/src/main/java/no/nav/melosys/service/aktoer/KontaktopplysningService.java`
   - **ProgressFile**: KontaktopplysningServiceKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

6. **File**: `service/src/test/kotlin/no/nav/melosys/service/aktoer/UtenlandskMyndighetServiceKtTest.kt`
   - **Java Test File**: `service/src/test/java/no/nav/melosys/service/aktoer/UtenlandskMyndighetServiceTest.java`
   - **Main Class File**: `service/src/main/java/no/nav/melosys/service/aktoer/UtenlandskMyndighetService.java`
   - **ProgressFile**: UtenlandskMyndighetServiceKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

### Service Module - Altinn (2 files)

7. **File**: `service/src/test/kotlin/no/nav/melosys/service/altinn/AltinnSoeknadServiceKtTest.kt`
   - **Java Test File**: `service/src/test/java/no/nav/melosys/service/altinn/AltinnSoeknadServiceTest.java`
   - **Main Class File**: `service/src/main/java/no/nav/melosys/service/altinn/AltinnSoeknadService.java`
   - **ProgressFile**: AltinnSoeknadServiceKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

8. **File**: `service/src/test/kotlin/no/nav/melosys/service/altinn/SoeknadMapperKtTest.kt`
   - **Java Test File**: `service/src/test/java/no/nav/melosys/service/altinn/SoeknadMapperTest.java`
   - **Main Class File**: `service/src/main/java/no/nav/melosys/service/altinn/SoeknadMapper.java`
   - **ProgressFile**: SoeknadMapperKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

### Service Module - Avklartefakta (5 files)

9. **File**: `service/src/test/kotlin/no/nav/melosys/service/avklartefakta/AvklartefaktaDtoKonvertererKtTest.kt`
   - **Java Test File**: `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklartefaktaDtoKonvertererTest.java`
   - **Main Class File**: `service/src/main/java/no/nav/melosys/service/avklartefakta/AvklartefaktaDtoKonverterer.java`
   - **ProgressFile**: AvklartefaktaDtoKonvertererKtTest-review-and-fixes.md
   - **Status**: Completed
   - **Verdict**: Issues found and fixed

10. **File**: `service/src/test/kotlin/no/nav/melosys/service/avklartefakta/AvklartefaktaServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklartefaktaServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/avklartefakta/AvklartefaktaService.java`
    - **ProgressFile**: AvklartefaktaServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

11. **File**: `service/src/test/kotlin/no/nav/melosys/service/avklartefakta/AvklarteMedfolgendeFamilieServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklarteMedfolgendeFamilieServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/avklartefakta/AvklarteMedfolgendeFamilieService.java`
    - **ProgressFile**: AvklarteMedfolgendeFamilieServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

12. **File**: `service/src/test/kotlin/no/nav/melosys/service/avklartefakta/AvklarteVirksomheterServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklarteVirksomheterServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/avklartefakta/AvklarteVirksomheterService.java`
    - **ProgressFile**: AvklarteVirksomheterServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

13. **File**: `service/src/test/kotlin/no/nav/melosys/service/avklartefakta/AvklartMaritimtArbeidKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/avklartefakta/AvklartMaritimtArbeidTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/avklartefakta/AvklartMaritimtArbeid.java`
    - **ProgressFile**: AvklartMaritimtArbeidKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

### Service Module - Behandling (6 files)

14. **File**: `service/src/test/kotlin/no/nav/melosys/service/behandling/AngiBehandlingsresultatServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/behandling/AngiBehandlingsresultatServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/behandling/AngiBehandlingsresultatService.java`
    - **ProgressFile**: AngiBehandlingsresultatServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

15. **File**: `service/src/test/kotlin/no/nav/melosys/service/behandling/BehandlingEventListenerKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/behandling/BehandlingEventListenerTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/behandling/BehandlingEventListener.java`
    - **ProgressFile**: BehandlingEventListenerKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

16. **File**: `service/src/test/kotlin/no/nav/melosys/service/behandling/BehandlingServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/behandling/BehandlingServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/behandling/BehandlingService.java`
    - **ProgressFile**: BehandlingServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed (partial due to file size)

17. **File**: `service/src/test/kotlin/no/nav/melosys/service/behandling/UtledMottaksdatoKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/behandling/UtledMottaksdatoTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/behandling/UtledMottaksdato.kt`
    - **ProgressFile**: UtledMottaksdatoKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

18. **File**: `service/src/test/kotlin/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingJobbKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingJobbTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingJobb.java`
    - **ProgressFile**: AvsluttArt13BehandlingJobbKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

19. **File**: `service/src/test/kotlin/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/behandling/jobb/AvsluttArt13BehandlingService.java`
    - **ProgressFile**: AvsluttArt13BehandlingServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

### Service Module - Brev (10 files)

20. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/BrevmalListeServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/BrevmalListeServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/BrevmalListeService.java`
    - **ProgressFile**: BrevmalListeServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

21. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/DokumentNavnServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/DokumentNavnServiceTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/brev/DokumentNavnService.kt`
    - **ProgressFile**: DokumentNavnServiceKtTest-review-and-fixes.md
    - **Status**: Processing
    - **Verdict**:

22. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/OppdaterUtkastServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/OppdaterUtkastServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/bestilling/OppdaterUtkastService.java`
    - **ProgressFile**: OppdaterUtkastServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

23. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/UtkastBrevServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/UtkastBrevServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/UtkastBrevService.java`
    - **ProgressFile**: UtkastBrevServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

24. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/bestilling/HentBrevmottakereNorskMyndighetServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/bestilling/HentBrevmottakereNorskMyndighetServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/bestilling/HentBrevmottakereNorskMyndighetService.java`
    - **ProgressFile**: HentBrevmottakereNorskMyndighetServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

25. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/bestilling/HentMuligeBrevmottakereServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/bestilling/HentMuligeBrevmottakereServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/bestilling/HentMuligeBrevmottakereService.java`
    - **ProgressFile**: HentMuligeBrevmottakereServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

26. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/bestilling/HentTilgjengeligeNorskeMyndigheterServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/bestilling/HentTilgjengeligeNorskeMyndigheterServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/bestilling/HentTilgjengeligeNorskeMyndigheterService.java`
    - **ProgressFile**: HentTilgjengeligeNorskeMyndigheterServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

27. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/bestilling/ProduserBrevServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/bestilling/ProduserBrevServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/bestilling/ProduserBrevService.java`
    - **ProgressFile**: ProduserBrevServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

28. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/bestilling/ProduserUtkastServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/bestilling/ProduserUtkastServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/brev/bestilling/ProduserUtkastService.java`
    - **ProgressFile**: ProduserUtkastServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

29. **File**: `service/src/test/kotlin/no/nav/melosys/service/brev/bestilling/TilBrevAdresseServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/brev/bestilling/TilBrevAdresseServiceTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/brev/bestilling/TilBrevAdresseService.kt`
    - **ProgressFile**: TilBrevAdresseServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

### Service Module - Dokument (45 files)

30. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/BostedGrunnlagKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/BostedGrunnlagTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/BostedGrunnlag.java`
    - **ProgressFile**: BostedGrunnlagKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

31. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/BrevmottakerServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/BrevmottakerServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/BrevmottakerService.java`
    - **ProgressFile**: BrevmottakerServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

32. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/DokgenServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/DokgenServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/DokgenService.java`
    - **ProgressFile**: DokgenServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

33. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/DokumentServiceFasadeKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/DokumentServiceFasadeTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/DokumentServiceFasade.java`
    - **ProgressFile**: DokumentServiceFasadeKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

34. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/DokumentServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/DokumentServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/DokumentService.java`
    - **ProgressFile**: DokumentServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

35. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/BrevDataMapperRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/BrevDataMapperRuterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/BrevDataMapperRuter.java`
    - **ProgressFile**: BrevDataMapperRuterKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: No improvements needed

36. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/BrevDataServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/BrevDataServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/BrevDataService.java`
    - **ProgressFile**: BrevDataServiceKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

[Continuing with files 37-124...]

37. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerA001KtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerA001Test.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerA001.java`
    - **ProgressFile**: BrevDataByggerA001KtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

38. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerA1KtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerA1Test.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerA1.java`
    - **ProgressFile**: BrevDataByggerA1KtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed
`
39. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAnmodningUnntakKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAnmodningUnntakTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAnmodningUnntak.java`
    - **ProgressFile**: BrevDataByggerAnmodningUnntakKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

40. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAvslagArbeidsgiverKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAvslagArbeidsgiverTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAvslagArbeidsgiver.java`
    - **ProgressFile**: BrevDataByggerAvslagArbeidsgiverKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

41. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAvslagYrkesaktivKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAvslagYrkesaktivTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerAvslagYrkesaktiv.java`
    - **ProgressFile**: BrevDataByggerAvslagYrkesaktivKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

42. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerInnvilgelseFlereLandKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerInnvilgelseFlereLandTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerInnvilgelseFlereLand.java`
    - **ProgressFile**: BrevDataByggerInnvilgelseFlereLandKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

43. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerInnvilgelseKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerInnvilgelseTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerInnvilgelse.java`
    - **ProgressFile**: BrevDataByggerInnvilgelseKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

44. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerStandardKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerStandardTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerStandard.java`
    - **ProgressFile**: BrevDataByggerStandardKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

45. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerUtpekingAnnetLandKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerUtpekingAnnetLandTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerUtpekingAnnetLand.java`
    - **ProgressFile**: BrevDataByggerUtpekingAnnetLandKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

46. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVedleggKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVedleggTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVedlegg.java`
    - **ProgressFile**: BrevDataByggerVedleggKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

47. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVelgerKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVelgerTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/BrevDataByggerVelger.java`
    - **ProgressFile**: BrevDataByggerVelgerKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

48. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVideresendKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVideresendTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/bygger/BrevDataByggerVideresend.java`
    - **ProgressFile**: BrevDataByggerVideresendKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

49. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/datagrunnlag/AvklarteVirksomheterGrunnlagKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/datagrunnlag/AvklarteVirksomheterGrunnlagTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/datagrunnlag/AvklarteVirksomheterGrunnlag.java`
    - **ProgressFile**: AvklarteVirksomheterGrunnlagKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

50. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/datagrunnlag/BrevdataGrunnlagFactoryKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/datagrunnlag/BrevdataGrunnlagFactoryTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/datagrunnlag/BrevdataGrunnlagFactory.java`
    - **ProgressFile**: BrevdataGrunnlagFactoryKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

51. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/datagrunnlag/BrevDataGrunnlagKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/datagrunnlag/BrevDataGrunnlagTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/datagrunnlag/BrevDataGrunnlag.java`
    - **ProgressFile**: BrevDataGrunnlagKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

52. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/A001MapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/A001MapperTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/dokument/brev/mapper/A001Mapper.kt`
    - **ProgressFile**: A001MapperKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed (added missing 5 tests)

53. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/AnmodningUnntakMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/AnmodningUnntakMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/AnmodningUnntakMapper.java`
    - **ProgressFile**: AnmodningUnntakMapperKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

54. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/AttestMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/AttestMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/AttestMapper.java`
    - **ProgressFile**: AttestMapperKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: No improvements needed

55. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/AvslagArbeidsgiverMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/AvslagArbeidsgiverMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/AvslagArbeidsgiverMapper.java`
    - **ProgressFile**: AvslagArbeidsgiverMapperKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

56. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/AvslagYrkesaktivMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/AvslagYrkesaktivMapperTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/dokument/brev/mapper/AvslagYrkesaktivMapper.kt`
    - **ProgressFile**: AvslagYrkesaktivMapperKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

57. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/BehandlingstypeKodeMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/BehandlingstypeKodeMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/BehandlingstypeKodeMapper.java`
    - **ProgressFile**: BehandlingstypeKodeMapperKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

58. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/DokgenMapperDatahenterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/DokgenMapperDatahenterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/DokgenMapperDatahenter.java`
    - **ProgressFile**: DokgenMapperDatahenterKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

59. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/DokgenServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/DokgenServiceTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/dokument/brev/mapper/DokgenService.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

60. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/DokumentproduksjonsInfoMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/DokumentproduksjonsInfoMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/DokumentproduksjonsInfoMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

61. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/felles/BrevMapperUtilsKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/felles/BrevMapperUtilsTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/felles/BrevMapperUtils.java`
    - **ProgressFile**: BrevMapperUtilsKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: Issues found and fixed

62. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/felles/FellesBrevtypeMappingKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/felles/FellesBrevtypeMappingTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/dokument/brev/mapper/felles/FellesBrevtypeMapping.kt`
    - **ProgressFile**: FellesBrevtypeMappingKtTest-review-and-fixes.md
    - **Status**: Completed
    - **Verdict**: No improvements needed

63. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/felles/VilkaarbegrunnelseFactoryKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/felles/VilkaarbegrunnelseFactoryTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/felles/VilkaarbegrunnelseFactory.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

64. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/InnvilgelseArbeidsgiverBrevMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/InnvilgelseArbeidsgiverBrevMapperTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/dokument/brev/mapper/InnvilgelseArbeidsgiverBrevMapper.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

65. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/InnvilgelsesbrevFlereLandMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/InnvilgelsesbrevFlereLandMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/InnvilgelsesbrevFlereLandMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

66. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/InnvilgelsesbrevMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/InnvilgelsesbrevMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/InnvilgelsesbrevMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

67. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/TrygdeavtaleAdresseSjekkerKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/TrygdeavtaleAdresseSjekkerTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/TrygdeavtaleAdresseSjekker.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

68. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/TrygdeavtaleMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/TrygdeavtaleMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/TrygdeavtaleMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

69. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/UtpekingAnnetLandMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/UtpekingAnnetLandMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/UtpekingAnnetLandMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

70. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/brev/mapper/VideresendSoknadMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/brev/mapper/VideresendSoknadMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/brev/mapper/VideresendSoknadMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

71. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/sed/EessiServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/sed/EessiServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/sed/EessiService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

72. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/sed/mapper/VilkaarsresultatTilBegrunnelseMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/sed/mapper/VilkaarsresultatTilBegrunnelseMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/sed/mapper/VilkaarsresultatTilBegrunnelseMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

73. **File**: `service/src/test/kotlin/no/nav/melosys/service/dokument/sed/SedDataGrunnlagFactoryKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/dokument/sed/SedDataGrunnlagFactoryTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/dokument/sed/SedDataGrunnlagFactory.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:
`
### Service Module - EESSI (8 files)

74. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/AdminFjernmottakerSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/AdminFjernmottakerSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/AdminFjernmottakerSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

75. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/AdminInnvalideringSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/AdminInnvalideringSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/AdminInnvalideringSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

76. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/AnmodningOmUnntakSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/AnmodningOmUnntakSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/AnmodningOmUnntakSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

77. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/ArbeidFlereLandSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/ArbeidFlereLandSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/ArbeidFlereLandSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

78. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/DefaultSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/DefaultSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/DefaultSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

79. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/SedGrunnlagMapperKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/SedGrunnlagMapperTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/eessi/SedGrunnlagMapper.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

80. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/SvarAnmodningUnntakSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/SvarAnmodningUnntakSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/SvarAnmodningUnntakSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

81. **File**: `service/src/test/kotlin/no/nav/melosys/service/eessi/UnntaksperiodeSedRuterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/eessi/UnntaksperiodeSedRuterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/eessi/UnntaksperiodeSedRuter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

### Service Module - Kontroll (5 files)

82. **File**: `service/src/test/kotlin/no/nav/melosys/service/kontroll/feature/godkjennunntak/UnntaksperiodeKontrollKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/kontroll/feature/godkjennunntak/UnntaksperiodeKontrollTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/kontroll/feature/godkjennunntak/UnntaksperiodeKontroll.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

83. **File**: `service/src/test/kotlin/no/nav/melosys/service/kontroll/regler/ArbeidsstedReglerKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/kontroll/regler/ArbeidsstedReglerTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/kontroll/regler/ArbeidsstedRegler.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

84. **File**: `service/src/test/kotlin/no/nav/melosys/service/kontroll/regler/overlapp/PeriodeOverlappSjekkKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/kontroll/regler/overlapp/PeriodeOverlappSjekkTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/kontroll/regler/overlapp/PeriodeOverlappSjekk.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

85. **File**: `service/src/test/kotlin/no/nav/melosys/service/kontroll/regler/PeriodeReglerKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/kontroll/regler/PeriodeReglerTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/kontroll/regler/PeriodeRegler.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

86. **File**: `service/src/test/kotlin/no/nav/melosys/service/kontroll/regler/UfmReglerKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/kontroll/regler/UfmReglerTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/kontroll/regler/UfmRegler.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

### Service Module - Persondata (9 files)

87. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/PersondataServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/PersondataServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/PersondataService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

88. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/familie/EktefelleEllerPartnerFamiliemedlemFilterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/familie/EktefelleEllerPartnerFamiliemedlemFilterTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/persondata/familie/EktefelleEllerPartnerFamiliemedlemFilter.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

89. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/familie/FamiliemedlemServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/familie/FamiliemedlemServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/familie/FamiliemedlemService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

90. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/FamiliemedlemOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/FamiliemedlemOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/FamiliemedlemOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

91. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/NavnOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/NavnOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/NavnOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

92. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/PersonMedHistorikkOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/PersonMedHistorikkOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/PersonMedHistorikkOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

93. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/PersonopplysningerOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/PersonopplysningerOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/PersonopplysningerOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

94. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/SivilstandOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/SivilstandOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/SivilstandOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

95. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/adresse/BostedsadresseOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/adresse/BostedsadresseOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/adresse/BostedsadresseOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

96. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/adresse/KontaktadresseOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/adresse/KontaktadresseOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/adresse/KontaktadresseOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

97. **File**: `service/src/test/kotlin/no/nav/melosys/service/persondata/mapping/adresse/OppholdsadresseOversetterKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/persondata/mapping/adresse/OppholdsadresseOversetterTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/persondata/mapping/adresse/OppholdsadresseOversetter.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

### Service Module - Registeropplysninger (4 files)

98. **File**: `service/src/test/kotlin/no/nav/melosys/service/registeropplysninger/OrganisasjonOppslagServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/registeropplysninger/OrganisasjonOppslagServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/registeropplysninger/OrganisasjonOppslagService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

99. **File**: `service/src/test/kotlin/no/nav/melosys/service/registeropplysninger/RegisteropplysningerPeriodeFactoryKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerPeriodeFactoryTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerPeriodeFactory.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

100. **File**: `service/src/test/kotlin/no/nav/melosys/service/registeropplysninger/RegisteropplysningerRequestKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerRequestTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerRequest.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

101. **File**: `service/src/test/kotlin/no/nav/melosys/service/registeropplysninger/RegisteropplysningerServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/registeropplysninger/RegisteropplysningerService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Sak (5 files)

102. **File**: `service/src/test/kotlin/no/nav/melosys/service/sak/ArkivsakServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/sak/ArkivsakServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/sak/ArkivsakService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

103. **File**: `service/src/test/kotlin/no/nav/melosys/service/sak/FagsakServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/sak/FagsakServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/sak/FagsakService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

104. **File**: `service/src/test/kotlin/no/nav/melosys/service/sak/OpprettSakKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/sak/OpprettSakTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/sak/OpprettSak.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

105. **File**: `service/src/test/kotlin/no/nav/melosys/service/sak/VideresendSoknadServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/sak/VideresendSoknadServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/sak/VideresendSoknadService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

106. **File**: `service/src/test/kotlin/no/nav/melosys/service/saksopplysninger/SaksopplysningEventListenerKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/saksopplysninger/SaksopplysningEventListenerTest.java`
     - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/saksopplysninger/SaksopplysningEventListener.kt`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Søknad (1 file)

107. **File**: `service/src/test/kotlin/no/nav/melosys/service/soknad/SoknadMottattKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/soknad/SoknadMottattTest.java`
     - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/soknad/SoknadMottatt.kt`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Tilgang (3 files)

108. **File**: `service/src/test/kotlin/no/nav/melosys/service/tilgang/AksesskontrollImplKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/tilgang/AksesskontrollImplTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/tilgang/AksesskontrollImpl.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

109. **File**: `service/src/test/kotlin/no/nav/melosys/service/tilgang/BrukertilgangKontrollKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/tilgang/BrukertilgangKontrollTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/tilgang/BrukertilgangKontroll.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

110. **File**: `service/src/test/kotlin/no/nav/melosys/service/tilgang/RedigerbarKontrollKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/tilgang/RedigerbarKontrollTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/tilgang/RedigerbarKontroll.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Trygdeavtale/Unntak/Utpeking (6 files)

111. **File**: `service/src/test/kotlin/no/nav/melosys/service/trygdeavtale/TrygdeavtaleServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/trygdeavtale/TrygdeavtaleServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/trygdeavtale/TrygdeavtaleService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

112. **File**: `service/src/test/kotlin/no/nav/melosys/service/unntak/AnmodningsperiodeServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/unntak/AnmodningsperiodeServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/unntak/AnmodningsperiodeService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

113. **File**: `service/src/test/kotlin/no/nav/melosys/service/unntak/AnmodningUnntakServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/unntak/AnmodningUnntakServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/unntak/AnmodningUnntakService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

114. **File**: `service/src/test/kotlin/no/nav/melosys/service/unntaksperiode/UnntaksperiodeServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/unntaksperiode/UnntaksperiodeServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/unntaksperiode/UnntaksperiodeService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

115. **File**: `service/src/test/kotlin/no/nav/melosys/service/utpeking/UtpekingServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/utpeking/UtpekingServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/utpeking/UtpekingService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Vedtak (3 files)

116. **File**: `service/src/test/kotlin/no/nav/melosys/service/vedtak/EosVedtakServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/vedtak/EosVedtakServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/vedtak/EosVedtakService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

117. **File**: `service/src/test/kotlin/no/nav/melosys/service/vedtak/TrygdeavtaleVedtakServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/vedtak/TrygdeavtaleVedtakServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/vedtak/TrygdeavtaleVedtakService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

118. **File**: `service/src/test/kotlin/no/nav/melosys/service/vedtak/VedtaksfattingFasadeKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/vedtak/VedtaksfattingFasadeTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/vedtak/VedtaksfattingFasade.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Vilkaar (1 file)

119. **File**: `service/src/test/kotlin/no/nav/melosys/service/vilkaar/InngangsvilkaarServiceKtTest.kt`
     - **Java Test File**: `service/src/test/java/no/nav/melosys/service/vilkaar/InngangsvilkaarServiceTest.java`
     - **Main Class File**: `service/src/main/java/no/nav/melosys/service/vilkaar/InngangsvilkaarService.java`
     - **ProgressFile**:
     - **Status**: Not started
     - **Verdict**:

### Service Module - Root Level Services (5 files)

120. **File**: `service/src/test/kotlin/no/nav/melosys/service/BehandlingsnotatServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/BehandlingsnotatServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/BehandlingsnotatService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

121. **File**: `service/src/test/kotlin/no/nav/melosys/service/LandvelgerServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/LandvelgerServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/LandvelgerService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

122. **File**: `service/src/test/kotlin/no/nav/melosys/service/OppfriskSaksopplysningerServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/OppfriskSaksopplysningerServiceTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/OppfriskSaksopplysningerService.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

123. **File**: `service/src/test/kotlin/no/nav/melosys/service/SaksopplysningerServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/SaksopplysningerServiceTest.java`
    - **Main Class File**: `service/src/main/kotlin/no/nav/melosys/service/SaksopplysningerService.kt`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

124. **File**: `service/src/test/kotlin/no/nav/melosys/service/UnntaksregistreringServiceKtTest.kt`
    - **Java Test File**: `service/src/test/java/no/nav/melosys/service/UnntaksregistreringServiceTest.java`
    - **Main Class File**: `service/src/main/java/no/nav/melosys/service/UnntaksregistreringService.java`
    - **ProgressFile**:
    - **Status**: Not started
    - **Verdict**:

## Notes
- This tracking file will be updated as each file is reviewed and fixed
- Each file will have a corresponding progress file created during review
- Verdicts: "No improvements needed", "Issues found and fixed"
