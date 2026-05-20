# APK Signing Setup

This document explains how to set up APK signing for GitHub Actions releases.

## Why Sign APKs?

- **Security**: Users can verify the app comes from you
- **Updates**: Android allows updates only from the same signing key
- **Trust**: Signed APKs are required for most app stores

## Setting Up Signing Keys

### Step 1: Create a Keystore

```bash
# Generate a new keystore
keytool -genkey -v \
  -keystore notes-app-release.keystore \
  -alias notesapp \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You'll be prompted for:
- **Keystore password**: Choose a strong password
- **Key password**: Can be same as keystore password
- **Name/Organization**: Your info

### Step 2: Base64 Encode the Keystore

```bash
# On Linux/macOS
base64 -i notes-app-release.keystore -o keystore-base64.txt

# Or copy to clipboard
base64 notes-app-release.keystore | pbcopy  # macOS
base64 notes-app-release.keystore | xclip -selection clipboard  # Linux
```

### Step 3: Add GitHub Secrets

Go to your GitHub repository:

1. **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `SIGNING_KEY` | Base64-encoded keystore content |
| `ALIAS` | Your key alias (e.g., `notesapp`) |
| `KEY_STORE_PASSWORD` | Your keystore password |
| `KEY_PASSWORD` | Your key password |

### Step 4: Test the Setup

1. Create a new release in GitHub
2. The workflow will automatically sign the APK
3. Download and verify the signed APK

## Verification

Users can verify the APK signature:

```bash
# Check APK signature
apksigner verify -v notes-app-X.X.X.apk

# Show certificate info
keytool -list -printcert -jarfile notes-app-X.X.X.apk
```

## Security Notes

⚠️ **Important:**
- **Never commit** the keystore file to git
- **Never share** your keystore password
- **Backup** your keystore file securely (password manager, encrypted storage)
- **If you lose the keystore**, you cannot publish updates to existing users

## Without Signing (Debug Builds)

If you don't set up signing:
- Debug APKs are still built automatically
- Release APKs will be unsigned (`*-unsigned.apk`)
- Unsigned APKs can be installed but won't update signed installs
- Suitable for testing, not for distribution

## Alternative: APK Signature Scheme v2

The workflow uses `apksigner` which automatically applies:
- **v1 scheme** (JAR signing) - backward compatible
- **v2 scheme** (APK signing) - modern, faster verification
- **v3 scheme** (if available) - rotation support

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Keystore was tampered with" | Wrong password or corrupted keystore |
| "Failed to load signer" | Wrong alias name |
| "Base64 decode error" | Ensure proper base64 encoding |
| Workflow fails silently | Check GitHub Actions logs |

## For Obtainium Users

Obtainium doesn't require signed APKs, but they are recommended for:
- Better Android system integration
- Seamless app updates
- User trust

The workflow creates both signed and debug APKs - Obtainium will pick up whichever is available.
