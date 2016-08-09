/*
 * Created 2015-12-3 20:35:58
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 *
 * @author Azige
 */
public enum SiteConfig{

    KONACHAN("Konachan.com", "http://konachan.com", "/konachan.com.png"),
    YANDERE("yande.re", "http://yande.re", "/yande.re.png");

    private String name;
    private String rootUrl;
    private String iconLocation;

    private SiteConfig(String name, String rootUrl, String iconLocation){
        this.name = name;
        this.rootUrl = rootUrl;
        this.iconLocation = iconLocation;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getRootUrl(){
        return rootUrl;
    }

    public void setRootUrl(String rootUrl){
        this.rootUrl = rootUrl;
    }

    public String getIconLocation(){
        return iconLocation;
    }

    public Image getIcon() throws IOException{
        return ImageIO.read(getClass().getResource(iconLocation));
    }
}
