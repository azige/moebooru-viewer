/*
 * Created 2015-12-13 15:30:57
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Azige
 */
public class Utils{

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private Utils(){
    }

    public static Image resizeImage(Image image, double maxWidth, double maxHeight){
        double limitRatio = maxWidth / maxHeight;
        double width = image.getWidth(null);
        double height = image.getHeight(null);
        double ratio = width / height;
        if (ratio > limitRatio){
            width = maxWidth;
            height = maxWidth / ratio;
        }else{
            height = maxHeight;
            width = maxHeight * ratio;
        }
        return image.getScaledInstance((int)width, (int)height, Image.SCALE_SMOOTH);
    }

    public static String getFileNameFromUrl(String url){
        try{
            return URLDecoder.decode(url.replaceFirst(".*/", ""), "UTF-8")
                .replaceAll("\\?.*", "")
                .replaceAll("[?:*\"<>|]", "_");
        }catch (UnsupportedEncodingException ex){
            logger.error("URL编码异常", ex);
            return "unknown";
        }
    }
}
