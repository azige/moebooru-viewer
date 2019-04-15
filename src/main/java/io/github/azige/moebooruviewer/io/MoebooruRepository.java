/*
 * Copyright (C) 2017 Azige
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.azige.moebooruviewer.io;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.function.DoubleConsumer;

import io.github.azige.moebooruviewer.Utils;
import io.github.azige.moebooruviewer.config.SiteConfig;
import io.github.azige.moebooruviewer.model.Post;
import io.reactivex.Single;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Representing a storage of Moebooru images.
 * The "storage" means a Moebooru site or a local cache.
 * <p>
 * Loading a image is to load a cached local file or download it from a Moebooru
 * site.
 *
 * @author Azige
 */
@Component
public class MoebooruRepository implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(MoebooruRepository.class);

    public static final String PREVIEW_DIR_NAME = "previews";
    public static final String SAMPLE_DIR_NAME = "samples";
    public static final String ORIGIN_DIR_NAME = "origins";

    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private NetIO netIO;

    private File cacheDir;
    private File previewDir;
    private File sampleDir;
    private File originDir;

    @Override
    public void afterPropertiesSet() throws Exception {
        cacheDir = new File(siteConfig.getName());
        previewDir = new File(cacheDir, PREVIEW_DIR_NAME);
        sampleDir = new File(cacheDir, SAMPLE_DIR_NAME);
        originDir = new File(cacheDir, ORIGIN_DIR_NAME);
    }

    public File getPreviewFile(Post post) {
        return new File(previewDir, post.getId() + ".jpg");
    }

    public Single<Image> loadPreviewAsync(Post post) {
        return loadPreviewAsync(post, true);
    }

    public Single<Image> loadPreviewAsync(Post post, boolean useCache) {
        long id = post.getId();
        File previewFile = new File(previewDir, id + ".jpg");
        return loadImageAsync(post.getPreviewUrl(), previewFile, useCache);
    }

    public File getSampleFile(Post post) {
        return new File(sampleDir, post.getId() + ".jpg");
    }

    public Single<Image> loadSampleAsync(Post post) {
        return loadSampleAsync(post, true);
    }

    public Single<Image> loadSampleAsync(Post post, boolean useCache) {
        long id = post.getId();
        File sampleFile = getSampleFile(post);
        return loadImageAsync(post.getSampleUrl(), sampleFile, useCache);
    }

    public Single<Image> loadSampleAsync(Post post, boolean useCache, DoubleConsumer progressHandler) {
        long id = post.getId();
        File sampleFile = getSampleFile(post);
        return loadImageAsync(post.getSampleUrl(), sampleFile, useCache, progressHandler);
    }

    public File getOriginFile(Post post) {
        try {
            return new File(originDir, URLDecoder.decode(post.getOriginUrl().replaceFirst(".*/", ""), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            LOG.error("URL编码异常", ex);
            return null;
        }
    }

    /**
     * Load a image asynchronously.
     *
     * @param url
     * @param localFile
     * @param useCache
     */
    public Single<Image> loadImageAsync(String url, File localFile, boolean useCache) {
        return loadImageAsync(url, localFile, useCache, it -> {});
    }

    public Single<Image> loadImageAsync(String url, File localFile, boolean useCache, DoubleConsumer progressHandler) {
        Single<Image> loadingSingle = Single.fromCallable(() -> Utils.loadImage(localFile));
        if (!useCache || !localFile.exists()) {
            return netIO.downloadFileAsync(url, localFile, progressHandler)
                .andThen(loadingSingle);
        } else {
            return loadingSingle;
        }
    }

    public boolean cleanCache() {
        try {
            FileUtils.deleteDirectory(cacheDir);
            return true;
        } catch (IOException ex) {
            LOG.warn("删除缓存目录失败！", ex);
            return false;
        }
    }
}
