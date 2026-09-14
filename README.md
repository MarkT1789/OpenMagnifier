# Open Magnifier

Last updated: September 8, 2026  
License: GPLv3

Open Magnifier is a privacy-first, open-source magnifier with Optical Character Recognition (OCR for Latin, Chinese, Devanagari, Japanese scripts/writing systems), Language Translation (Bengali, Chinese, English, French, Hindi, Indonesian, Japanese, Marathi, Portuguese, Spanish, Tagalog, Vietnamese), Output-Only Language Translation (Arabic, Urdu), speech output, and synchronized readout text banner.

Magnify → OCR (on-device) → Translate (on-device) → Speech + Readout Text Banner.

## Key Features

* **Immersive Experience.** True full-screen view without UI clutter.
* **Privacy First.** No ads, no tracking, no logging, no data collection.
* **Open Source.** Full transparency under the GPLv3 license.
* **On-Device.** All image processing occurs on the device. No images are sent to the cloud.
* **Configurable.** Because everyone's different, or as an old colleague used to say, “One size fits one.”
* **Freeze frame.** Lock the image to inspect small text without camera shake. Once frozen, you can pan and zoom the image.
* **Digital zoom.** Magnify both the live camera and the freeze frame.
* **Color filters.** High-contrast modes including inverse, grayscale, inverse grayscale, and black-yellow.
* **Display Mirroring.** Native landscape orientation lock ensures a good fit on external monitors without black bars.
* **Flashlight.** Enable the flashlight during live viewing to brighten images.
* **Speak Image (OCR + TTS).** Use on-device optical character recognition (OCR) to speak text on an image. Supports Latin, Chinese, Devanagari, and Japanese scripts/writing systems.
* **Language Translation.** Translate OCR text into 12 different languages: Bengali, Chinese, English, French, Hindi, Indonesian, Japanese, Marathi, Portuguese, Spanish, Tagalog, or Vietnamese. Destinations for output-only language translation include 2 languages: Arabic and Urdu.
* **Limited network connections.** Network connections are only used to download Translation models or Text-to-Speech voices.
* **Share Image.** Receive shared images from Google Photos, Messages, or any other app.
* **Visual Text Tracking.** A green bounding box highlights the currently spoken text block directly on the freeze-frame image so you can easily follow along visually.
* **Readout Banner.** Display spoken text with optional word highlighting in a text banner. Set the font size, font color, font typeface, highlight color, and the number of display lines.
* **Clipboard.** Copy OCR and language translation output to the system clipboard.
* **Help Overlay.** On-screen help for first-time users.

## What's New

* **1.17 Text to Speech options.** Added options to change the speech rate and pitch.
* **1.16 Help Overlay text options.** Added options to change the text size, text color, text font, and clear timeout.
* **1.15 Help Overlay.** Added a help overlay that provides temporary on-screen help for first-time users. Can be permanently disabled or enabled in Preferences.
* **1.14 Preference reorganization.** Reorganized preferences into categories.
* **1.13 Clipboard options.** Added options to copy OCR and language translation output to the system clipboard.
* **1.12 Text banner highlight options.** Added options to highlight the currently speaking word in the text banner.
* **1.11 Text banner font options.** Added font options for the text banner (Atkinson Hyperlegible Next, Open Dyslexic, Lexend Deca/Giga/Petra/Zetta).
* **1.10 Text banner color options.** Added text and background color options for the text banner.
* **1.9 Text banner line options.** Allows the text banner to display one, two, or three lines of text.
* **1.8 Arabic and Urdu translation support.** Supported for destination/output language translation only.
* **1.7 Volume keys, new languages, text size preference.** Use hardware volume keys to control speaking (volume up = repeat current item, volume down = speak next item, double volume up = speak previous item). Added Tagalog and Vietnamese translation options. Added banner font size preference.
* **1.6 Text banner.** Display recognized or translated text in a small single line text banner as it is spoken. Requires TextToSpeech support for onStartRange.
* **1.5 Share Image from other apps.** Receive shared images from Google Photos or Messages or any other app. Single images only.
* **1.4 Language translation for OCR.** Translate OCR text into ten different languages: Bengali, Chinese, English, French, Hindi, Indonesian, Japanese, Marathi, Portuguese, or Spanish. Requires a Google Play device and a network connection to download the translation model and voice.
* **1.3 Speak Image for Chinese, Devanagari, and Japanese.** Added support for Chinese, Devanagari, and Japanese scripts for on-device optical character recognition (OCR).
* **1.2 Speak Image for Latin.** Uses on-device optical character recognition (OCR) to speak text on a frozen image. Latin characters only. Speaking occurs two seconds after the image is idle.
* **1.1 Flashlight.** Enable the flashlight during live viewing. Automatically turns off during freeze frame. Multiple levels of brightness are supported on devices that support it (requires Android 13).

## Supported Setups

* **Handheld:** Use as a portable magnifying glass.
* **Stationary/Mounted:** Mount the device and mirror to a monitor for a desktop CCTV experience.
* **Keyboard-Controlled:** Use a Bluetooth or USB keyboard for precise navigation without touching the screen.

## Quick Tutorial

* **Toggle between live and freeze frame:** Single tap the screen or press Space on the keyboard.
* **Adjust zoom:** Pinch the screen with two fingers or press Z on the keyboard.
* **Scroll (freeze frame):** Swipe the screen or press the arrow keys on the keyboard.
* **Display help (live view):** Long press the screen or press H on the keyboard.
* **Sharing image:** Live view, on-screen help, and preferences are not available.
* **Optical Character Recognition:** Disabled by default. Enable Speak Image in Preferences to read written text (Latin, Chinese, Devanagari, Japanese). Optical character recognition starts after a two-second inactivity timeout. A green bounding box outlines the active text block on the image as it is spoken. Speech is in the default system/device locale unless language translation is used.
* **Language Translation:** Disabled by default. Enable Speak Image in Preferences for written text (Latin, Chinese, Devanagari, Japanese). Enable Source and Destination Languages in Preferences. Speech is in the destination language. A 30 MB translation model needs to be downloaded for the source and destination languages if never used before.
* **Display spoken text:** Disabled by default. Enable Display Text Banner in Preferences to set one, two, or three lines of text. Text Banner Size changes the font size. Text Banner Color changes the text and background colors. Text Banner Font changes the font typeface. Text Banner Word Highlight changes the highlighting of the current speaking word.
* **Clipboard:** Disabled by default. Enable Clipboard in Preferences to save the OCR or language translation to the system clipboard. Wait for speech, then exit Open Magnifier or switch to live view to save the text to the system clipboard. Switching into live view is only supported if not using the share image feature. The device keyboard may display multiple clipboard history items (this is a feature of the keyboard).
* **Help Overlay:** Temporarily enabled by default but will be automatically disabled after a few interactions.

## Touchscreen Gestures

### Live view

* Single tap = switch to freeze frame.
* Two-finger pinch = change zoom.
* Double tap = open preferences.
* Long tap = show help.

### Freeze frame

* Single tap = switch to live view.
* Two-finger pinch = change zoom.
* Double tap = zoom in or out.
* Long tap = stop speech.
* Swipe or drag = scroll/pan.
* Double tap and hold and drag = change zoom (one finger version).
* Two-finger pinch and hold and drag = change zoom and scroll/pan.

## Keyboard Commands

* Space bar = toggle between live and freeze frame.
* z/Z = increase/decrease zoom level (live and freeze frame).
* Arrow keys = scroll (freeze frame).
* Shift + arrow = scroll to the edge (freeze frame).
* b/B = increase/decrease brightness (freeze frame).
* c/C = increase/decrease contrast (freeze frame).
* f/F = change the color filter (freeze frame).
* h = show help.
* l = toggle the flashlight or increase/decrease flashlight brightness.
* p = show preferences.
* r/R = increase/decrease rotation (freeze frame).
* s = changes speak image.
* v = show app version.
* x/X = increase/decrease left/right scroll percentage (freeze frame).
* y/Y = increase/decrease up/down scroll percentage (freeze frame).

## Hardware Volume Keys

* Requires Volume Keys and Speak Image enabled in Preferences.
* Volume up = speak current item and stop.
* Double volume up = speak previous item and stop.
* Volume down = speak next item and stop.

## Frequently Asked Questions (FAQ)

* **What devices are supported?** Any Android device running Android 7 (API 24) or greater. Tested on Pixel 8a (Android 17), Amazon Fire 10 (FireOS/Android 11), and Pixel 2 (Android 11).
* **Why is it locked to Landscape?** To optimize for external monitors. Most TVs and monitors are landscape; locking the app prevents “pillarboxing” or awkward stretching when mirroring.
* **What is the “Rotation” feature for?** Hardware manufacturers sometimes mount camera sensors at odd angles. If your freeze frame image looks sideways, use R to fix it. This is also great for viewing portrait-style documents on a horizontal screen.
* **What is the purpose of the shift arrow key commands?** To allow quick navigation to the edge of the image. This is useful when scrolling to the end of the line and then needing to quickly return to the start of the next line.
* **Why does brightness make my image darker?** When the color filter is inverted, brightness behavior is also inverted, that is, increasing the brightness setting actually decreases the display brightness.
* **How can I get higher quality images when zooming?** Image quality depends on your device's camera hardware.
* **Why does the flashlight stay on for a few seconds after freezing the image?** This is to help slower camera hardware take the snapshot before the flashlight is disabled.
* **Why is the live view slow to update when the flashlight is enabled?** Turning on the flashlight can cause the display to flash as the camera hardware tries to adjust the white balance. To avoid this, the live view has a slight delay.

## Pro Tips

* **External Displays:** Because the app is locked to Landscape, it’s perfect for plugging your Android device into a TV or spare monitor for mirroring your display. This effectively turns your tablet into a high-powered desktop magnifier.
* **Reading Long Text:** Use Shift + Left Arrow to quickly snap back to the beginning of a line after you’ve scrolled to the end of the previous one.
* **Legacy Devices:** If you have an old Pixel 2 or an Amazon Fire tablet lying around, this app is light enough to give those devices a second life as dedicated accessibility tools.

## Device Dependencies

* **Optical Character Recognition:** Bundled. No dependencies. Available on all devices.
* **Atkinson Hyperlegible, Lexend, Open Dyslexic fonts:** Bundled. No dependencies. Available on all devices.
* **Flashlight brightness levels:** Requires Android 13 and hardware support.
* **Language translation:** Requires Google Play device support and a network connection to download the model.
* **Text banner:** Requires TextToSpeech onStartRange API support.
* **Text to Speech languages:** Requires Text to Speech language support and a network connection to download the voice data.

## Settings Breakdown

### Camera Settings

* Camera Zoom Level (1.0x, 1.25x, 1.5x, 2.0x, 3.0x)
* Screen Rotation (0, 90, 180, 270)
* Flashlight (Off, 20%, 40%, 60%, 80%, 100%)

### Image Settings

* Color Filters (None, Grayscale, Inverse, Inverse grayscale, Black Yellow)
* Brightness Boost (Disabled, Low, Medium, High)
* Contrast Boost (Disabled, Low, Medium, High)

### Optical Character Recognition

* Speak Image Script (Disabled, Latin, Chinese, Devanagari, Japanese)
* Volume Key Controls (Disabled, Enabled)

### Language Translation Settings

* Source/Input Language (Disabled, Bengali, Chinese, English, French, Hindi, Indonesian, Japanese, Marathi, Portuguese, Spanish, Tagalog, Vietnamese)
* Destination/Output Language (Disabled, Arabic, Bengali, Chinese, English, French, Hindi, Indonesian, Japanese, Marathi, Portuguese, Spanish, Tagalog, Urdu, Vietnamese)

### Text Banner Settings

* Display Text Banner (Disabled, One line, Two lines, Three lines)
* Text Banner Size (0.67x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x, 2.5x)
* Text Banner Color (White text/black background, Black text/white background, Yellow text/black background, Black text/yellow background, Accessibility safe: deep indigo text/sky blue background, Solar eclipse: soft yellow text/dark slate gray background, Warm sand: dark espresso text/soft cream background)
* Text Banner Font (system sans serif, Atkinson Hyperlegible Next Medium, Atkinson Hyperlegible Next Bold, Open Dyslexic Regular, Open Dyslexic Bold, Lexend Deca Medium, Lexend Deca Bold, Lexend Giga Medium, Lexend Giga Bold, Lexend Petra Medium, Lexend Petra Bold, Lexend Zetta Medium, Lexend Zetta Bold)
* Text Banner Word Highlight (Disabled, Underline, Soft green highlight, Deep blue highlight, Soft green highlight/underline, Deep blue highlight/underline, Soft green font, Deep blue font, Soft green font/underline, Deep blue font/underline)

### General Settings

* Clipboard (Disabled, Enabled)

### Text to Speech Settings

* Speech Rate (Default, 0.5x, 0.8x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x)
* Pitch (Default, 0.5, 0.75, 1.0, 1.25, 1.5)

### Help Overlay Settings

* Help Overlay (Disabled, Temporarily Enabled, Enabled)
* Help Text Size (0.75x, 1.0x, 1.25x, 1.5x)
* Help Text Color (White, Black, Yellow, Green)
* Help Text Font (system sans serif, Atkinson Hyperlegible Next Medium, Atkinson Hyperlegible Next Bold, Open Dyslexic Regular, Open Dyslexic Bold, Lexend Deca Medium, Lexend Deca Bold, Lexend Giga Medium, Lexend Giga Bold, Lexend Petra Medium, Lexend Petra Bold, Lexend Zetta Medium, Lexend Zetta Bold)
* Help Clear Timer (5 seconds, 15 seconds, 30 seconds)

### Keyboard Command Settings

* Horizontal Scroll Percentage (10%, 20%, 25%, 33%, 50%)
* Vertical Scroll Percentage (10%, 20%, 25%, 33%, 50%)

## Links & Resources

* [**License**](https://uglydog.io/magnifier/license.html)
* [**Privacy Policy**](https://uglydog.io/magnifier/privacy.html)
* [**Release notes and APKs**](https://uglydog.io/magnifier/code.html)
* [**Google Play App Listing**](https://play.google.com/store/apps/details?id=io.uglydog.magnifier)
* [**GitHub source code**](https://github.com/MarkT1789/OpenMagnifier)

## Backstory

Here is the origin story of the Open Magnifier app:

When I first attended the CSUN Assistive Technology Conference many years ago, I noticed a number of vendors selling really expensive hardware video magnifiers and I always wondered if there was a cheaper option. I considered implementing a solution for an accessibility hackathon. My initial thought was using two cheap Amazon Fire tablets (USD $50 each), one for the camera and one for the display, connected via Bluetooth and held together by a 3D-printed frame. But then I realized a USB serial cable would be easier and more efficient. Then I discovered that my Google Pixel 8a phone supports DisplayPort Alt Mode, which allows screen mirroring to a USB-C computer monitor with just a basic USB-C video cable. I tried using the Google Magnifier app, which worked but the app is locked to portrait orientation which limited the usefulness with a standard 16:9 monitor. So I started developing my own magnifier app, locked to landscape orientation with keyboard support. See the “Low-Cost Video Magnifier Setups” section below which describes two possible solutions.

During the GitHub Open Source Assistive Technology Hackathon in May 2026, an attendee recommended tailoring my magnifier app for people in low-resource regions. As a result, I selected OCR scripts and translation languages designed to maximize access for global communities with limited resources.

## Low-Cost Video Magnifier Setups

* **Pixel 8a connected to a USB-C computer monitor.** My test setup is an LG 32-inch 4K monitor with a built-in USB-C hub (LG 32UP83A-W, around $400 USD on Amazon). Connect a keyboard directly to the monitor and control the Open Magnifier app with keyboard commands. The monitor also supplies power to the phone, which is a great feature.
* **Pixel 8a connected to a USB-C hub.** My test setup is an Anker Model 332 USB-C hub (around $18 USD on Amazon). Connect the Pixel 8a, keyboard, and HDMI monitor to the hub.
* **Notes:**
  * **Cables.** Not all USB-C cables are the same. Use a video cable (USB-C 3.2 Gen 2, USB4, Thunderbolt 3 or 4).
  * **DisplayPort Alt Mode.** Pixel 8a supports DisplayPort Alt Mode which allows the mirroring of the screen using USB-C. Not all phones support this but most will support some type of screen mirroring.
  * **Audio Output.** Sound typically outputs through the connected computer monitor. If there is no sound, then check the monitor's volume settings.

## Target Audience

* **Individuals with Low Vision:** People who require digital magnification, freeze-frame zooming, high-contrast color filters, and Text-to-Speech with on-image green bounding box tracking to visually locate recognized text.
* **Users with Macular Degeneration:** People with central vision loss who depend on high-contrast color modes, digital magnification, and Text-to-Speech with word highlighting to read around central blind spots (scotomas).
* **People with Dyslexia or Reading Disabilities:** Users who benefit from specialized accessibility fonts (Open Dyslexic, Atkinson Hyperlegible, Lexend), customizable line banners, and word-by-word highlighted text playback.
* **Budget-Conscious Users and Low-Resource Communities:** Individuals who cannot afford high-cost (USD $1,000+) proprietary CCTV video magnifiers and want to convert standard smartphones or older Android devices into desktop magnifiers using external monitors and keyboards.
* **Users in Under-Resourced & Developing Regions:** Communities in low-income or infrastructure-limited areas who require zero-cost (GPLv3) tools with fully offline, on-device processing and localized script/language support (such as Devanagari, Bengali, Urdu, and Tagalog) that operate reliably on low-cost or repurposed legacy devices without needing continuous internet connectivity.
* **Multilingual Readers & Immigrants:** Non-native readers needing offline visual translation across varied scripts and global languages, including Devanagari, Hindi, Bengali, Tagalog, Vietnamese, Urdu, and Arabic.
* **Privacy-Conscious Users:** Privacy advocates who prefer open-source software (GPLv3) that processes image and OCR data entirely on-device without cloud tracking or logging.
* **Users Seeking Independence from Proprietary Ecosystems:** Right-to-repair advocates and open-source enthusiasts who want to avoid expensive, single-purpose assistive hardware by building modular, easily repairable magnifiers using standard consumer electronics and open-source (GPLv3) software.
* **Users with a Narrow Field of Vision (Tunnel Vision):** Individuals with conditions such as glaucoma or retinitis pigmentosa who rely on customizable 1-3 line text banners, pan-and-scan freeze-framing, and clutter-free full-screen layouts to keep text strictly within their restricted field of view.
* **Travelers and General Utility Consumers:** Everyday consumers who benefit from the “curb-cut effect” by using data-free OCR, live translation, and flashlight modes to easily navigate maps, transit schedules, and low-light environments while traveling.

## Open Source Libraries

* **Subsampling Scale Image View**  
  Copyright © 2018 Dave Morrissey.  
  Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
* **Google ML Kit Text Recognition**  
  Copyright © 2026 Google LLC. All rights reserved.  
  Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Font Licenses

* **Atkinson Hyperlegible**  
  Copyright © 2019 Braille Institute of America, Inc.  
  Licensed under the [SIL Open Font License, v1.1](https://scripts.sil.org/OFL).
* **Lexend**  
  Copyright © 2018 The Lexend Project Authors.  
  Licensed under the [SIL Open Font License, v1.1](https://scripts.sil.org/OFL).
* **OpenDyslexic**  
  Copyright © 2011–2022 Abelardo Gonzalez.  
  Licensed under the [Creative Commons Attribution 4.0 International License (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/).

## Acknowledgements

* [**Icon Kitchen**](https://icon.kitchen/)
* [**Subsampling Scale Image View**](https://github.com/davemorrissey/subsampling-scale-image-view)
* [**Atkinson Hyperlegible Next Fonts**](https://www.brailleinstitute.org/freefont/)
* [**Lexend Deca/Giga/Petra/Zetta Fonts**](https://www.lexend.com/)
* [**Open Dyslexic Fonts**](https://opendyslexic.org/)

## Disclaimers

This app utilizes Google ML Kit Text Recognition to convert images into text. While we strive for high accuracy, optical character recognition (OCR) technology is inherently subject to limitations. The accuracy of the text generated depends heavily on external factors, including image quality, lighting conditions, camera focus, font styles, and handwritten text. Open Magnifier does not guarantee 100% accuracy, completeness, or reliability of the recognized text. Users should always manually review and verify the extracted text before relying on it for any critical, financial, legal, or medical purposes.

Under no circumstances shall the developers of Open Magnifier be held liable for any direct, indirect, incidental, or consequential damages, losses, or errors arising from the use of, or the inability to use, the OCR feature. This includes, but is not limited to, data loss, misinterpretation of text, or transcription errors.

This service may contain translations powered by Google. Google disclaims all warranties related to the translations, express or implied, including any warranties of accuracy, reliability, and any implied warranties of merchantability, fitness for a particular purpose and noninfringement.

## About

Open Magnifier Copyright © 2026 Mark Tamura

This program is free software: you can redistribute and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation (GPLv3 or later).

Read the complete [Open Magnifier license](https://uglydog.io/magnifier/license.html) for details.
