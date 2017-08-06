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

import io.github.azige.moebooruviewer.Post;
import io.github.azige.moebooruviewer.SiteConfig;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Representing a storage of Moebooru images.
 * The "storage" means a Moebooru site or a local cache.
 * <p>
 * Loading a image is to load a cached local file or download it from a Moebooru site.
 *
 * @author Azige
 */
@Component
public class MoebooruRepository{

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

    @PostConstruct
    private void init(){
        cacheDir = new File(siteConfig.getName());
        previewDir = new File(cacheDir, PREVIEW_DIR_NAME);
        sampleDir = new File(cacheDir, SAMPLE_DIR_NAME);
        originDir = new File(cacheDir, ORIGIN_DIR_NAME);
    }

    public File getPreviewFile(Post post){
        return new File(previewDir, post.getId() + ".jpg");
    }

    public void loadPreviewAsync(Post post, Consumer<Image> callback){
        loadPreviewAsync(post, true, callback);
    }

    public void loadPreviewAsync(Post post, boolean useCache, Consumer<Image> callback){
        long id = post.getId();
        File previewFile = new File(previewDir, id + ".jpg");
        loadImageAsync(post.getPreviewUrl(), previewFile, useCache, callback);
    }

    public File getSampleFile(Post post){
        return new File(sampleDir, post.getId() + ".jpg");
    }

    public void loadSampleAsync(Post post, Consumer<Image> callback){
        loadSampleAsync(post, true, callback);
    }

    public void loadSampleAsync(Post post, boolean useCache, Consumer<Image> callback){
        long id = post.getId();
        File sampleFile = getSampleFile(post);
        loadImageAsync(post.getSampleUrl(), sampleFile, useCache, callback);
    }

    public File getOriginFile(Post post){
        try{
            return new File(originDir, URLDecoder.decode(post.getOriginUrl().replaceFirst(".*/", ""), "UTF-8"));
        }catch (UnsupportedEncodingException ex){
            LOG.error("URL编码异常", ex);
            return null;
        }
    }

    /**
     * Load a image asynchronously.
     * The callback will be invoked with loaded image when succeeded
     * or null when failed.
     *
     * @param url
     * @param localFile
     * @param useCache
     * @param callback
     */
    public void loadImageAsync(String url, File localFile, boolean useCache, Consumer<Image> callback){
        if (!useCache || !localFile.exists()){
            netIO.downloadFileAsync(url, localFile, new DownloadCallbackAdapter(){
                @Override
                public void onComplete(File file){
                    callback.accept(loadImage(file));
                }

                @Override
                public void onFail(Exception ex){
                    callback.accept(null);
                }
            });
        }else{
            callback.accept(loadImage(localFile));
        }
    }

    private Image loadImage(File file){
        for (int i = 0; i < 5; i++){
            try{
                // 偶尔有无法确认的 NullPointerException
                synchronized (ImageIO.class){
                    return ImageIO.read(file);
                }
            }catch (IOException ex){
                LOG.warn("读取本地文件出错", ex);
            }
        }
        return null;
    }

    public boolean cleanCache(){
        try{
            FileUtils.deleteDirectory(cacheDir);
            return true;
        }catch (IOException ex){
            LOG.warn("删除缓存目录失败！", ex);
            return false;
        }
    }
}
