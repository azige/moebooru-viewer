/*
 * Created 2015-11-29 0:45:28
 */
package io.github.azige.moebooruviewer.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.google.common.net.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
public class NetIO{

    private static final Logger logger = LoggerFactory.getLogger(NetIO.class);

    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ClientHttpRequestFactory requestFactory;

    private int maxRetryCount = 5;
    private boolean forceHttps = true;
    private boolean closed = false;
    private ReferenceQueue<ClientHttpResponse> referenceQueue = new ReferenceQueue<>();
    private Set<WeakReference<ClientHttpResponse>> responseRefs = Collections.synchronizedSet(new HashSet<>());
    private Thread responseRefCleanThread;

    public NetIO(){
    }

    @PostConstruct
    private void init(){
        responseRefCleanThread = new Thread(() -> {
            while (true){
                try{
                    Reference<? extends ClientHttpResponse> ref = referenceQueue.remove();
                    responseRefs.remove(ref);
                }catch (InterruptedException ex){
                    return;
                }
            }
        });
        responseRefCleanThread.setName("Response references clean thread");
        responseRefCleanThread.setDaemon(true);
        responseRefCleanThread.start();
    }

    @PreDestroy
    private void destroy(){
        closed = true;
        responseRefCleanThread.interrupt();
        responseRefs.forEach(ref -> {
            ClientHttpResponse response = ref.get();
            if (response != null){
                response.close();
            }
        });
    }

    private ClientHttpRequest createRequest(String url) throws IOException{
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
        try{
            ClientHttpRequest request = requestFactory.createRequest(u.toURI(), HttpMethod.GET);
            logger.info("start downloading: {}", u);
            return request;
        }catch (URISyntaxException ex){
            throw new RuntimeException(ex);
        }
    }

    public byte[] download(String url){
        for (int retryCount = 0; retryCount < maxRetryCount; retryCount++){
            try{
                ClientHttpRequest request = createRequest(url);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (ClientHttpResponse response = request.execute()){
                    responseRefs.add(new WeakReference<>(response, referenceQueue));
                    InputStream input = response.getBody();
                    IOUtils.copy(input, output);
                }
                return output.toByteArray();
            }catch (IOException ex){
                if (ex instanceof SocketException && closed){
                    return null;
                }
                logger.info("IO异常，重试", ex);
            }
        }
        logger.info("到达最大重试次数，已放弃重试");
        return null;
    }

    public void downloadFile(String url, File fileToSave){
        try{
            downloadFileAsync(url, fileToSave, new DownloadCallbackAdapter()).get();
        }catch (InterruptedException ex){
            throw new RuntimeException(ex);
        }catch (ExecutionException ex){
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException){
                throw (RuntimeException)cause;
            }else{
                throw new RuntimeException(ex);
            }
        }
    }

    public Future<?> downloadFileAsync(String url, File fileToSave, DownloadCallback callback){
        Objects.requireNonNull(url);
        Objects.requireNonNull(fileToSave);
        DownloadCallback cb;
        if (callback == null){
            cb = new DownloadCallbackAdapter();
        }else{
            cb = callback;
        }
        return executorService.submit(() -> {
            long totalContentCount = -1;
            long downloadedCount = 0;
            double currentRate = 0;
            // 64KB buffer
            int bufferSize = 1 << 16;
            byte[] buffer = new byte[bufferSize];
            File dir = fileToSave.getParentFile();
            if (!dir.exists()){
                dir.mkdirs();
            }
            if (fileToSave.exists()){
                fileToSave.delete();
            }
            Exception lastException = null;
            for (int retryCount = 0; retryCount < maxRetryCount; retryCount++){
                try{
                    ClientHttpRequest request = createRequest(url);
                    if (downloadedCount > 0){
                        request.getHeaders().add(HttpHeaders.RANGE, "bytes=" + downloadedCount + "-");
                    }
                    // HTTP connecting can not be interrupted
                    try (ClientHttpResponse response = request.execute()){
                        if (closed){
                            logger.info("程序已终止");
                            return;
                        }
                        responseRefs.add(new WeakReference<>(response, referenceQueue));
                        logger.info("response HTTP {} for {}", response.getRawStatusCode(), url);
                        if (totalContentCount == -1){
                            totalContentCount = Long.parseLong(response.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH));
                        }
                        try (InputStream input = new BufferedInputStream(response.getBody(), bufferSize); OutputStream output = new FileOutputStream(fileToSave, true)){
                            int readCount;
                            while ((readCount = input.read(buffer)) != -1){
                                output.write(buffer, 0, readCount);
                                downloadedCount += readCount;
                                double rate = (double)downloadedCount / totalContentCount;
                                if (rate - currentRate > 0.01){
                                    currentRate = rate;
                                    cb.onProgress(currentRate);
                                }
                            }
                        }
                    }
                    logger.info("downloaded: {} to {}", url, fileToSave);
                    cb.onComplete(fileToSave);
                    return;
                }catch (IOException ex){
                    logger.info("IO异常", ex);
                    if (closed){
                        logger.info("程序已终止，不再重试");
                        return;
                    }
                    logger.info("进行重试，第 {} 次", retryCount + 1);
                    lastException = ex;
                }
            }
            logger.info("到达最大重试次数，已放弃重试");
            cb.onFail(lastException);
        });
    }
}
