/*
 * Created 2015-11-29 0:45:28
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
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

    public interface DownloadListener extends EventListener{

        void onProgress(double rate);

        void onComplete();

        void onFail();
    }

    public static final String PREVIEW_DIR_NAME = "previews";
    public static final String SAMPLE_DIR_NAME = "samples";
    public static final String ORIGIN_DIR_NAME = "origins";
    public static final String TAG_DIR_NAME = "tags";

    private static final Logger logger = LoggerFactory.getLogger(NetIO.class);

    @Autowired
    private SiteConfig siteConfig;

    private int maxRetryCount = 5;
    private File cacheDir;
    private File previewDir;
    private File sampleDir;
    private File originDir;
    private boolean forceHttps = true;
    private final Map<File, Object> fileLockerMap = new HashMap<>();

    public NetIO(){
    }

    @PostConstruct
    private void init(){
        cacheDir = new File(siteConfig.getName());
        previewDir = new File(cacheDir, PREVIEW_DIR_NAME);
        sampleDir = new File(cacheDir, SAMPLE_DIR_NAME);
        originDir = new File(cacheDir, ORIGIN_DIR_NAME);
//        Stream.of(previewDir, sampleDir, originDir)
//            .filter(dir -> !dir.exists())
//            .forEach(File::mkdirs);
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

    public InputStream openStream(String url) throws IOException{
        InputStream inputStream = openConnection(url).getInputStream();
        return inputStream;
    }

    private URLConnection openConnection(String url) throws IOException{
        URL u;
        // quick fix for that URLs from konachan.net API does not contain protocol
        if (url.startsWith("//")){
            url = "http:" + url;
        }
        if (forceHttps){
            u = new URL(url.replace("http:", "https:"));
        }else{
            u = new URL(url);
        }
        logger.info("downloading: {}", u);
        URLConnection connection = u.openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        return connection;
    }

    public Image loadPreview(Post post){
        return loadPreview(post, false);
    }

    public Image loadPreview(Post post, boolean force){
        long id = post.getId();
        File previewFile = new File(previewDir, id + ".jpg");
        return loadImage(previewFile, post.getPreviewUrl(), force);
    }

    public File getSampleFile(Post post){
        return new File(sampleDir, post.getId() + ".jpg");
    }

    public Image loadSample(Post post){
        return loadSample(post, false);
    }

    public Image loadSample(Post post, boolean force){
        long id = post.getId();
        File sampleFile = getSampleFile(post);
        return loadImage(sampleFile, post.getSampleUrl(), force);
    }

    public File getOriginFile(Post post){
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
        File originFile = getOriginFile(post);
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

    public Runnable createDownloadTask(File localFile, String url, DownloadListener listener){
        Objects.requireNonNull(localFile);
        Objects.requireNonNull(url);
        Objects.requireNonNull(listener);
        return () -> {

            try{
                URLConnection connection = openConnection(url);
                long contentLength = connection.getContentLengthLong();

                // 64KB buffer
                int bufferSize = 1 << 16;
                byte[] buffer = new byte[bufferSize];
                long downloadedLength = 0;
                double currentRate = 0;
                int readBytes;
                File dir = localFile.getParentFile();
                if (!dir.exists()){
                    dir.mkdirs();
                }
                try (InputStream input = new BufferedInputStream(connection.getInputStream(), bufferSize); OutputStream output = new FileOutputStream(localFile)){
                    while ((readBytes = input.read(buffer)) != -1){
                        output.write(buffer, 0, readBytes);
                        downloadedLength += readBytes;
                        double rate = (double)downloadedLength / contentLength;
                        if (rate - currentRate > 0.01){
                            currentRate = rate;
                            listener.onProgress(currentRate);
                        }
                    }
                }
                listener.onComplete();
                logger.info("downloaded: {}", localFile);
            }catch (MalformedURLException ex){
                logger.error("URL编码异常", ex);
                listener.onFail();
            }catch (IOException ex){
                logger.info("IO异常", ex);
                listener.onFail();
            }
        };
    }

    /**
     * 下载一个文件。如果本地已有则不下载。
     *
     * @param localFile 下载到的本地文件
     * @param url       要下载的资源 url
     * @return 下载成功则为 true，否则为 flase
     */
    public boolean cacheFile(File localFile, String url){
        return cacheFile(localFile, url, false);
    }

    /**
     * 下载一个文件。如果本地已有则不下载。可以指定强制下载
     *
     * @param localFile 下载到的本地文件
     * @param url       要下载的资源 url
     * @param force     若为 true，则即使文件已存在也重新下载
     * @return 下载成功则为 true，否则为 flase
     */
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
                File dir = localFile.getParentFile();
                if (!dir.exists()){
                    dir.mkdirs();
                }
                Object flag = retry(() -> {
                    try (InputStream input = openStream(url); OutputStream output = new BufferedOutputStream(new FileOutputStream(localFile))){
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

    public boolean cleanCache(){
        try{
            FileUtils.deleteDirectory(cacheDir);
            return true;
        }catch (IOException ex){
            logger.warn("删除缓存目录失败！", ex);
            return false;
        }
    }
}
