/*
 * Created 2015-12-13 15:30:57
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import io.github.azige.moebooruviewer.config.UserSetting;
import io.github.azige.moebooruviewer.config.UserSetting.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Azige
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    public static Image resizeImage(Image image, double maxWidth, double maxHeight) {
        double limitRatio = maxWidth / maxHeight;
        double width = image.getWidth(null);
        double height = image.getHeight(null);
        double ratio = width / height;
        if (ratio > limitRatio) {
            width = maxWidth;
            height = maxWidth / ratio;
        } else {
            height = maxHeight;
            width = maxHeight * ratio;
        }
        return image.getScaledInstance((int) width, (int) height, Image.SCALE_SMOOTH);
    }

    public static String getFileNameFromUrl(String url) {
        try {
            return URLDecoder.decode(url.replaceFirst(".*/", ""), "UTF-8")
                .replaceAll("\\?.*", "")
                .replaceAll("[?:*\"<>|]", "_");
        } catch (UnsupportedEncodingException ex) {
            logger.error("URL编码异常", ex);
            return "unknown";
        }
    }

    public static Image loadImage(File file) {
        for (int i = 0; i < 5; i++) {
            try {
                // 偶尔有无法确认的 NullPointerException
                synchronized (ImageIO.class) {
                    return ImageIO.read(file);
                }
            } catch (IOException ex) {
                logger.warn("读取本地文件出错", ex);
            }
        }
        return null;
    }

    public static void configProxy(UserSetting userSetting) {
        if (userSetting.isProxyEnabled()) {
            ProxyConfig proxyConfig = userSetting.getProxyConfig();
            List<Proxy> proxies = Arrays.asList(new Proxy(Type.SOCKS, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())));
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return proxies;
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    logger.error("连接代理服务器出错，uri={}, address={}, ex={}", uri, sa, ioe);
                }
            });
        } else {
            ProxySelector.setDefault(null);
        }
    }
}
