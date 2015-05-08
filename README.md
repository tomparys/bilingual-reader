# Bilingual Reader for Android

## Abstract 

* Version: 0.7
* License: The GNU General Public License v3.0, see the [`COPYRIGHT.md`](COPYRIGHT.md) file.
* Forked from the [EPUB3reader](https://github.com/pettarin/epub3reader) project.

Bilingual Reader is an app for Android that allows the user to read a book in two languages at the same time.
The user provides two EPUB ebooks, each of which contains the same book but in a different language, the app facilitates their parallel reading.


### Download, Installation, and Privacy/Security Notes

From this repository, you can download:

* the Eclipse project directory, ready to be imported in your Eclipse workspace, from the `bilingual-reader` directory
* some sample EPUB eBooks containing parallel texts, from the `ebooks` directory
* some screenshots, from the `screenshots` directory
* source code for the Epublib library used in the project, from the `third-party` directory
* Android Support Library v7 appcompat also used in the project, from the `third-party/android-support-v7-appcompat` directory.

To use the app on your smartphone or tablet, you need:

* Android 4.0 or higher

The app needs the following permissions:

* Storage (needed to read your EPUB files)
* Network communications (needed to access/display Web resources referenced by your EPUB files)
* Development tools (needed for debug/logging purposes)

_Important Note_: JavaScript is _enabled_ by default because it allows lot of interesting things in EPUB 3 eBooks. However, JavaScript might also be exploited by an attacker to damage your device. Hence, you should not load EPUB files obtained from untrusted sources. The permission to access the network is required because we want to support browsing remote resources linked from within EPUB books. We value _your_ privacy as much as _ours_: you are absolutely welcome to recompile the app from the source code, in case you want to disable some of the aforementioned features.


### Usage Tips

When you open the app, you will be presented with a list of EPUB files found on your device. Tap on one to open it.

If you want to open a second book, open the navigation drawer (by tapping on the app name in the upper left corner) and click on `Open Book 2`.

To turn chapter, just swipe left or right.

If you have opened two eBooks containing the same book in different languages (or one ebook that contains parallel texts [see section below]), you can activate parallel reading functions selecting `Sync Chapters` and `Sync Scroll` in the menu. The two book panels on the screen will be synchronized: when you turn chapter in one of the two panels, the other will be updated as well.

To close one of the panels, just tap the small semi-transparent square in the upper right corner.

A single tap on a link will follow that link. A long tap will open its target in the other panel (useful when you have footnotes or link to remote Web sites).


### Compilation

You are welcome to compile the app for yourself. Start by adding the project to your IDE (We use Eclipse) and then add the Android Support Library v7 appcompat which you can find at `third-party/android-support-v7-appcompat/` following the instructions in the [`third-party/android-support-v7-appcompat/README/README`](third-party/android-support-v7-appcompat/README/README) file.


### Naming Convention for Parallel Texts in a single eBook

To enable the parallel text function, the eBook producer must signal the correspondence between XHTML contents to the reading system.

We implemented the following simple rule: just name two corresponding parts `filename.XX.xhtml` and `filename.YY.xhtml`, where `XX` and `YY` are two valid [ISO 639-1 language codes](http://www.iso.org/iso/home/standards/language_codes.htm). For example, `chapter1.en.xhtml` and `chapter1.it.xhtml` indicate the English and the Italian version of the first chapter of the book. If an XHTML page does not have a language code, it is considered "common" to all the languages present in the EPUB eBook, and rendered according to the order defined in the spine. This might be useful for frontmatter (e.g., a title page) or backmatter materials (e.g., a bibliography), which might be common to more than a single language.

For example, an EPUB file with the following spine:

```
title.xhtml
toc.en.xhtml
chapter1.en.xhtml
chapter2.en.xhtml
chapter3.en.xhtml
end.en.xhtml
toc.it.xhtml
chapter1.it.xhtml
chapter2.it.xhtml
chapter3.it.xhtml
end.it.xhtml
toc.de.xhtml
chapter1.de.xhtml
chapter2.de.xhtml
chapter3.de.xhtml
end.de.xhtml
appendix1.xhtml
appendix2.xhtml
```

will show a common title page (`title.xhtml`), then the TOC (`toc.??.xhtml`), three chapters (`chapter?.??.xhtml`), and an end page (`end.??.xhtml`) in English, Italian, and German, and then two common appendices (`appendix1.xhtml` and `appendix2.xhtml`). As the above example shows, more than two languages might be contained in the same EPUB eBook: since the apps currently supports only two panels, it will ask the user for selecting two languages out of those contained in the EPUB file.

You can find some working examples (i.e., EPUB files) in the `ebooks/` directory.

We chose a naming convention for ease in implementing it, but other (possibly, more powerful) markup-based conventions can be devised. Observe that by adopting a naming convention (within the EPUB specification) we ensure backward compatibility with other EPUB reading systems not supporting special rendering for parallel texts.
