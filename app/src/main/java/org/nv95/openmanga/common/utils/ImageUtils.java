package org.nv95.openmanga.common.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import org.nv95.openmanga.R;
import org.nv95.openmanga.common.TransitionDisplayer;
import org.nv95.openmanga.common.utils.network.AppImageDownloader;
import org.nv95.openmanga.core.storage.settings.AppSettings;

import java.io.File;

/**
 * Created by koitharu on 24.12.17.
 */

public abstract class ImageUtils {

	private static DisplayImageOptions.Builder sOptionsThumbnail = null;
	private static DisplayImageOptions.Builder sOptionsUpdate = null;

	public static void init(Context context) {
		sOptionsThumbnail = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.resetViewBeforeLoading(true);

		if (!ImageLoader.getInstance().isInited()) {
			final int cacheMb = AppSettings.get(context).getCacheMaxSizeMb();
			ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
					.defaultDisplayImageOptions(sOptionsThumbnail.build())
					.diskCacheSize(cacheMb * 1024 * 1024)
					.imageDownloader(new AppImageDownloader(context))
					.memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // 2 Mb
					.build();

			ImageLoader.getInstance().init(config);
		}
		Drawable holder = ContextCompat.getDrawable(context, R.drawable.placeholder);
		sOptionsThumbnail
				.showImageOnFail(holder)
				.showImageForEmptyUri(holder)
				.showImageOnLoading(holder)
				.displayer(new FadeInBitmapDisplayer(500, true, true, false));

		sOptionsUpdate = new DisplayImageOptions.Builder()
				.cacheInMemory(false)
				.cacheOnDisk(true)
				.resetViewBeforeLoading(false)
				.showImageOnLoading(null)
				.displayer(new TransitionDisplayer());
	}

	@Nullable
	public static Bitmap getCachedImage(String url) {
		try {
			Bitmap b = ImageLoader.getInstance().getMemoryCache().get(url);
			if (b == null) {
				File f = ImageLoader.getInstance().getDiskCache().get(url);
				if (f != null) {
					b = BitmapFactory.decodeFile(f.getPath());
				}
			}
			return b;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void setThumbnail(@NonNull ImageView imageView, String url, String referer) {
		if (url != null && url.equals(imageView.getTag())) {
			return;
		}
		imageView.setTag(url);
		ImageLoader.getInstance().displayImage(
				url,
				new ImageViewAware(imageView),
				sOptionsThumbnail
						.extraForDownloader(referer)
						.build()
		);
	}

	public static void setThumbnail(@NonNull ImageView imageView, @Nullable File file) {
		final String url = file == null ? null : "file://" + file.getPath();
		setThumbnail(imageView, url, null);
	}

	public static void setThumbnailCropped(@NonNull ImageView imageView, @Nullable String url, @NonNull MetricsUtils.Size size, String referer) {
		if (url != null && url.equals(imageView.getTag())) {
			return;
		}
		imageView.setTag(url);
		ImageLoader.getInstance().displayImage(
				url,
				new ImageViewAware(imageView),
				sOptionsThumbnail
						.extraForDownloader(referer)
						.build(),
				new ImageSize(size.width, size.height),
				null, null
		);
	}

	public static void setThumbnailCropped(@NonNull ImageView imageView, @Nullable File file, @NonNull MetricsUtils.Size size) {
		final String url = file != null && file.exists() ? "file://" + file.getPath() : null;
		setThumbnailCropped(imageView, url, size, null);
	}

	public static void setEmptyThumbnail(ImageView imageView) {
		ImageLoader.getInstance().cancelDisplayTask(imageView);
		imageView.setImageResource(R.drawable.placeholder);
		imageView.setTag(null);
	}

	public static void recycle(@NonNull ImageView imageView) {
		ImageLoader.getInstance().cancelDisplayTask(imageView);
		final Drawable drawable = imageView.getDrawable();
		if (drawable != null && drawable instanceof BitmapDrawable) {
			//((BitmapDrawable) drawable).getBitmap().recycle();
			imageView.setImageDrawable(null);
		}
		imageView.setTag(null);
	}

	public static void updateImage(@NonNull ImageView imageView, String url, String referer) {
		ImageLoader.getInstance().displayImage(
				url,
				imageView,
				sOptionsUpdate
						.extraForDownloader(referer)
						.build());
	}

	@Nullable
	public static Bitmap getThumbnail(String path, int width, int height) {
		Bitmap bitmap = getCachedImage(path);
		if (bitmap == null && path.startsWith("/")) {
			bitmap = BitmapFactory.decodeFile(path);
		}
		if (bitmap != null) {
			bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		}
		return bitmap;
	}
}
