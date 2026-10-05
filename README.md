# Menosan
Menosan is an Android app that helps households in Dumaguete City cut down on waste at the source. People log the waste their household throws away, by hand or with a photo. At the end of each week they get a private report that shows where most of their waste comes from, with small, practical ways to reduce it. The following week's report shows whether the changes they tried made a difference.

This repository is the Android app. It:

- signs users in with their Google account
- logs waste by hand, even offline, and syncs the entries once the phone is back online
- fills in log entries from a photo, which the user checks before saving
- shows the weekly reports, the biggest sources of waste, and ideas to try
- builds a short offline summary when a week ends without a connection
- lets users export or delete all of their data

All data stays private to each account. There are no rankings or comparisons between households.

## Built with

Kotlin, Jetpack Compose, Material 3, Hilt, Room, WorkManager, Retrofit, and Firebase Authentication. Requires Android 8.0 or later.

The app bundles the Roboto font, licensed under the SIL Open Font License 1.1 (see [`licenses/Roboto-OFL.txt`](licenses/Roboto-OFL.txt)).

## Download
[Menosan APK]([https://example.com](https://drive.google.com/drive/folders/1MOcX1rN7PNDpJaPWK8Z_i1UyUL2ScjZ7?usp=sharing)

