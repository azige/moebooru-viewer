/*
 * Created 2015-11-29 0:45:28
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Azige
 */
public class NetIO{

    @FunctionalInterface
    public interface SupplierThrowsIOException<S>{

        S supply() throws IOException;
    }

    @FunctionalInterface
    public interface RunnableThrowsIOException{

        void run() throws IOException;
    }

    public static final String PREVIEW_DIR_NAME = "previews";
    public static final String SAMPLE_DIR_NAME = "samples";
    public static final String ORIGIN_DIR_NAME = "origins";

    private static final Logger logger = LoggerFactory.getLogger(NetIO.class);
    private int maxRetryCount = 5;
    private File cacheDir;
    private File previewDir;
    private File sampleDir;
    private File originDir;
    private boolean forceHttps = true;
    private final Map<File, Object> fileLockerMap = new HashMap<>();

    public NetIO(File cacheDir){
        this.cacheDir = cacheDir;
        previewDir = new File(cacheDir, PREVIEW_DIR_NAME);
        if (!previewDir.exists()){
            previewDir.mkdirs();
        }
        sampleDir = new File(cacheDir, SAMPLE_DIR_NAME);
        if (!sampleDir.exists()){
            sampleDir.mkdirs();
        }
        originDir = new File(cacheDir, ORIGIN_DIR_NAME);
        if (!originDir.exists()){
            originDir.mkdirs();
        }
    }

    public <S> S retry(SupplierThrowsIOException<S> supplier){
        int count = 0;
        while (true){
            try{
                return supplier.supply();
            }catch (IOException ex){
                if (++count > maxRetryCount){
                    logger.info("IO异常，放弃重试", ex);
                    return null;
                }
                logger.info("IO异常，重试", ex);
            }
        }
    }

    public void retry(RunnableThrowsIOException runnable){
        retry(() -> {
            runnable.run();
            return null;
        });
    }

    public InputStream openStream(URL url){
        return retry(() -> {
            URL u;
            if (forceHttps){
                u = new URL(url.toString().replace("http:", "https:"));
            }else{
                u = url;
            }
            logger.info("downloading: {}", u);
            URLConnection connection = u.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            return connection.getInputStream();
        });
    }

    public Image loadPreview(Post post){
        long id = post.getId();
        File previewFile = new File(previewDir, id + ".jpg");
        return loadImage(previewFile, post.getPreviewUrl());
    }

    public Image loadSample(Post post){
        long id = post.getId();
        File sampleFile = new File(sampleDir, id + ".jpg");
        return loadImage(sampleFile, post.getSampleUrl());
    }

    public Image loadOrigin(Post post){
        try{
            File originFile = new File(originDir, URLDecoder.decode(post.getOriginUrl().replaceFirst(".*/", ""), "UTF-8"));
            return loadImage(originFile, post.getOriginUrl());
        }catch (UnsupportedEncodingException ex){
            logger.error("编码异常", ex);
            return null;
        }
    }

    public Image loadImage(File localFile, String url){
        Object locker;
        synchronized (fileLockerMap){
            locker = fileLockerMap.get(localFile);
            if (locker == null){
                locker = new Object();
                fileLockerMap.put(localFile, locker);
            }
        }
        synchronized (locker){
            if (!localFile.exists()){
                retry(() -> {
                    try (InputStream input = openStream(new URL(url)); OutputStream output = new FileOutputStream(localFile)){
                        IOUtils.copy(input, output);
                        logger.info("downloaded: {}", localFile);
                    }
                });
            }
            try{
                return ImageIO.read(localFile);
            }catch (IOException ex){
                logger.warn("无法打开本地图片", ex);
                return null;
            }
        }
    }
}
