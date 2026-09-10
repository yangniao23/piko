# Custom patches in this fork

This fork preserves upstream Piko and adds the independently buildable cellular-only data saver patch under `custom/cellular-saver/`.

See [the Japanese installation and build guide](custom/cellular-saver/README-ja.md).

- Target: `com.twitter.android`, `12.19.1-release.0` only.
- APK patching and signature verification passed; on-device behavior is not yet verified.
- The additional patch is built separately from the upstream Piko bundle. Adding this repository as a Morphe source alone does not make the custom patch available until its MPP is published and configured.
- No application APK, signing key, login token, or user data is committed.

The upstream build remains unchanged. Use `custom/cellular-saver/build.py` to build the additional MPP.
