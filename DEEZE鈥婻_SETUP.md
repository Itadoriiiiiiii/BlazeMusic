# Deezer integration

The BlazeMusic project is configured with the Deezer App ID provided for this build: `6231363283`.

OAuth redirect URI:
`blazemusic://deezer/callback`

The Android manifest already handles this callback. The app requests `basic_access,manage_library`, exchanges the OAuth code for an access token, then synchronizes the user playlists.

If Deezer rejects the OAuth application or redirect URI, the restriction comes from the Deezer developer platform; the App ID itself is already inserted into the project.

The integration synchronizes playlist metadata only. It does not download or decrypt Deezer subscription audio.
