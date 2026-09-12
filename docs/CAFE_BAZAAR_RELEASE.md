# Cafe Bazaar Signed APK Pipeline

The `Build signed Cafe Bazaar APK` GitHub Actions workflow produces the release artifact. It runs manually through **Actions → Build signed Cafe Bazaar APK → Run workflow**, and automatically for tags matching `v*`.

## Signing secrets

The workflow requires these repository-level GitHub Actions secrets:

| Secret | Value |
|---|---|
| `CAFE_BAZAAR_KEYSTORE_BASE64` | Single-line base64 encoding of the PKCS12/JKS keystore |
| `CAFE_BAZAAR_KEYSTORE_PASSWORD` | Keystore password |
| `CAFE_BAZAAR_KEY_ALIAS` | Private-key alias |
| `CAFE_BAZAAR_KEY_PASSWORD` | Private-key password |

The generated Hero Defense key has been installed in these four secrets. The private keystore and passwords are never stored in Git, Gradle files, workflow source, logs, or build artifacts. GitHub decrypts them only for the release job, which writes the keystore under `$RUNNER_TEMP` with mode `0600` and deletes it in an `always()` cleanup step.

The project owner receives a separate `HeroDefense-release-signing-bundle.zip`. Back it up securely in at least two private locations. Losing this key can make it impossible to publish updates under the same Cafe Bazaar application identity.

## Release checks

Before upload, the workflow:

1. Validates the Gradle wrapper and Java 17 environment.
2. Runs the complete core suite, including the 100-wave balance and reward-card simulations.
3. Runs Android release lint.
4. Builds `:android:assembleRelease` with the secret-backed signing configuration.
5. Uses Android SDK `apksigner` to verify the APK and print its certificate fingerprints.
6. Confirms package ID `com.amirrezahadipoor.herodefense`, a non-empty version, all three supported libGDX ABIs, and an APK size below 100 MB.
7. Uploads `HeroDefense-<version>-cafe-bazaar.apk` plus its SHA-256 checksum as a 90-day GitHub Actions artifact.

The output is an APK, not an Android App Bundle, because the requested release target is Cafe Bazaar.

## Verified execution

Workflow run [`34717926965`](https://github.com/amirrezahadipoor/Herodefense/actions/runs/34717926965) passed on commit `8c9ec38` on 2026-09-13. Core tests, release lint, signed assembly, APK verification, artifact upload, and keystore cleanup all passed. The uploaded artifact is `hero-defense-0.1.0-cafe-bazaar` (24,123,865-byte artifact archive; GitHub artifact SHA-256 `be3ec0301197f690c09159ac5bdd9931b161ddfb4e160b66a552ee45a9f391fb`). Android `apksigner` confirmed one RSA-4096 signer plus valid v1 and v2 signatures. The signing certificate SHA-256 is `e89cc045c432edddc52ffcc7faf5d6be92efc9cd352ace19896213415be87b53`.

## Local equivalent

Keep all values outside the repository, then run:

```sh
export CAFE_BAZAAR_KEYSTORE_PATH=/private/path/hero-defense-release.p12
export CAFE_BAZAAR_KEYSTORE_PASSWORD='...'
export CAFE_BAZAAR_KEY_ALIAS='hero-defense-release'
export CAFE_BAZAAR_KEY_PASSWORD='...'
./scripts/gradle.sh :android:assembleRelease
```

Do not create or commit `local.properties`, keystores, password files, or base64 copies in this repository.
