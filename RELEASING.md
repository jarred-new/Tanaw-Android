# Tanaw Android v1.0 release

The first stable Android release is version `1.0` (Android version code `1`).
The `Android v1.0 Release` workflow runs when the `v1.0` tag is pushed. It
builds and tests the app, signs and verifies the release APK, calculates its
SHA-256 checksum, and publishes both files as a stable GitHub Release.

## Signing secrets

Before pushing the release tag, configure these Actions repository secrets:

- `KEYSTORE_BASE64`: the release keystore file encoded as one line of Base64
- `KEYSTORE_PASSWORD`: the keystore password
- `KEY_PASSWORD`: the signing key password
- `KEY_ALIAS`: the signing key alias

On Linux or macOS, encode the keystore without line breaks:

```sh
base64 -w 0 release-keystore.jks
```

On macOS, use `base64 < release-keystore.jks | tr -d '\n'` instead. Store the
result as `KEYSTORE_BASE64` in the repository's Actions secrets. Never commit
the keystore, its passwords, or the encoded keystore value.

The APK is signed with the repository's release key. Preserve that key
securely: future APK updates must use the same key to update an existing
installation.

## Publish v1.0

After merging the release-ready changes to `main` and configuring the secrets,
create and push the release tag from that commit:

```sh
git switch main
git pull --ff-only
git tag -a v1.0 -m "Tanaw Android v1.0"
git push origin v1.0
```

The workflow publishes the release only after the tests pass and the signed APK
verifies successfully. Check the Actions run and the resulting GitHub Release
before announcing the download.
