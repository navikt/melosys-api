# Missing Test Files Conversion Tracking

## 7-Step Conversion Process (STRICT)
1. **Look at Java test and the class being tested**
2. **Convert with best practices** (MockK, Kotest, Kotlin idioms)
3. **Run the tests** to ensure they compile and pass
4. **Review the conversion** for completeness
5. **Document** in this tracking file
6. **Commit** with descriptive message
7. **Move to next test**

## Files to Convert

| # | Java File | Kotlin File (Expected) | Status | Comments |
|---|-----------|------------------------|--------|----------|
| 1 | InnhentingAvInntektsopplysningerMapperTest.java | InnhentingAvInntektsopplysningerMapperKtTest.kt | ❌ Not Found | Java file does not exist |
| 2 | PersondataServiceTest.java | PersondataServiceKtTest.kt | ✅ Already Converted | File exists at service/src/test/kotlin/../PersondataServiceKtTest.kt |
| 3 | KontrollServiceTest.java | KontrollServiceKtTest.kt | ❌ Not Found | Java file does not exist |
| 4 | NavnOversetterTest.java | NavnOversetterKtTest.kt | ✅ Already Converted | Java: ../mapping/NavnOversetterTest.java, Kotlin exists |
| 5 | PersonopplysningerOversetterTest.java | PersonopplysningerOversetterKtTest.kt | ✅ Already Converted | Java: ../mapping/PersonopplysningerOversetterTest.java, Kotlin exists |
| 6 | RegisteropplysningerMapperTest.java | RegisteropplysningerMapperKtTest.kt | ❌ Not Found | Java file does not exist |
| 7 | SakshistorikkServiceTest.java | SakshistorikkServiceKtTest.kt | ❌ Not Found | Java file does not exist |
| 8 | VedtakServiceTest.java | VedtakServiceKtTest.kt | ❌ Not Found | Java file does not exist (might be VedtaksfattingFasadeTest) |
| 9 | FtrlVedtakServiceTest.java | FtrlVedtakServiceTest.kt | ✅ Already Converted | Exists at service/src/test/kotlin/../vedtak/FtrlVedtakServiceTest.kt |
| 10 | InformasjonTrygdeavgiftMapperTest.java | InformasjonTrygdeavgiftMapperKtTest.kt | ❌ Not Found | Java file does not exist |

## Conversion Progress

### File 1: DokgenServiceTest (from mapper package) ✅ COMPLETED
- **Step 1**: ✅ Looked at Java test and DokgenService class
- **Step 2**: ✅ Converted to Kotlin with MockK, Kotest, and Kotlin idioms  
- **Step 3**: ✅ ACTUALLY RAN TESTS - All 24 tests now passing
- **Step 4**: ✅ All 24 tests converted (100% coverage)
- **Step 5**: ✅ Documented conversion status
- **Step 6**: ⏳ Ready to commit
- **Step 7**: ⏳ Ready for next test

**Critical Issues Fixed After Running Tests**:
1. Mock ordering issue: `saksbehandlerService.hentNavnForIdent` - specific mock must come after generic
2. Verification approach: Changed from `withArg` to slot capture for complex argument verification
3. All 24 tests verified as ACTUALLY PASSING via mvn test

---

*Each file will be updated as we progress through the conversion*