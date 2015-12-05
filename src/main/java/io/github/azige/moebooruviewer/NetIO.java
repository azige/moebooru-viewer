/*
 * Created 2015-11-29 0:45:28
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.io.BufferedOutputStream;
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
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
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
    public static final String TAG_DIR_NAME = "tags";

    private static final Logger logger = LoggerFactory.getLogger(NetIO.class);

    @Autowired
    private SiteConfig siteConfig;

    private int maxRetryCount = 5;
    private File previewDir;
    private File sampleDir;
    private File originDir;
    private boolean forceHttps = true;
    private final Map<File, Object> fileLockerMap = new HashMap<>();

    public NetIO(){
    }

    @PostConstruct
    private void init(){
        File cacheDir = new File(siteConfig.getName());
        previewDir = new File(cacheDir, PREVIEW_DIR_NAME);
        sampleDir = new File(cacheDir, SAMPLE_DIR_NAME);
        originDir = new File(cacheDir, ORIGIN_DIR_NAME);
        Stream.of(previewDir, sampleDir, originDir)
            .filter(dir -> !dir.exists())
            .forEach(File::mkdirs);
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

    public InputStream openStream(URL url) throws IOException{
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
    }

    public Image loadPreview(Post post){
        return loadPreview(post, false);
    }

    public Image loadPreview(Post post, boolean force){
        long id = post.getId();
        File previewFile = new File(previewDir, id + ".jpg");
        return loadImage(previewFile, post.getPreviewUrl(), force);
    }

    public Image loadSample(Post post){
        return loadSample(post, false);
    }

    public Image loadSample(Post post, boolean force){
        long id = post.getId();
        File sampleFile = new File(sampleDir, id + ".jpg");
        return loadImage(sampleFile, post.getSampleUrl(), force);
    }

    public File getOriginFileCache(Post post){
        try{
            return new File(originDir, URLDecoder.decode(post.getOriginUrl().replaceFirst(".*/", ""), "UTF-8"));
        }catch (UnsupportedEncodingException ex){
            logger.error("URL编码异常", ex);
            return null;
        }
    }

    public Image loadOrigin(Post post){
        return loadOrigin(post, false);
    }

    public Image loadOrigin(Post post, boolean force){
        File originFile = getOriginFileCache(post);
        return loadImage(originFile, post.getOriginUrl(), force);
    }

    public Image loadImage(File localFile, String url, boolean force){
        if (cacheFile(localFile, url, force)){
            for (int i = 0; i < 5; i++){
                try{
                    // 偶尔有无法确认的 NullPointerException
                    synchronized (ImageIO.class){
                        return ImageIO.read(localFile);
                    }
                }catch (IOException ex){
                    logger.warn("读取本地文件出错", ex);
                }
            }
        }
        return null;
    }

    public boolean cacheFile(File localFile, String url){
        return cacheFile(localFile, url, false);
    }

    public boolean cacheFile(File localFile, String url, boolean force){
        Object locker;
        synchronized (fileLockerMap){
            locker = fileLockerMap.get(localFile);
            if (locker == null){
                locker = new Object();
                fileLockerMap.put(localFile, locker);
            }
        }
        synchronized (locker){
            if (!localFile.exists() || force){
                Object flag = retry(() -> {
                    try (InputStream input = openStream(new URL(url)); OutputStream output = new BufferedOutputStream(new FileOutputStream(localFile))){
                        IOUtils.copy(input, output);
                        logger.info("downloaded: {}", localFile);
                        return new Object();
                    }
                });
                if (flag != null){
                    return true;
                }else{
                    localFile.delete();
                    return false;
                }
            }else{
                return true;
            }
        }
    }
}
