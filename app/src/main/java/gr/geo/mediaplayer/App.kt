package gr.geo.mediaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import gr.geo.mediaplayer.data.LibraryStore

class App : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        LibraryStore.init(this)
    }

    /** Coil image loader that can play animated GIFs. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(ImageDecoderDecoder.Factory()) }
            .crossfade(true)
            .build()
}
