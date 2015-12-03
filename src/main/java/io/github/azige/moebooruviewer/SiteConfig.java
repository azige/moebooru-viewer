/*
 * Created 2015-12-3 20:35:58
 */
package io.github.azige.moebooruviewer;

/**
 *
 * @author Azige
 */
public class SiteConfig{

    public static final SiteConfig KONACHAN = new SiteConfig("Konachan.com", "http://konachan.com");
    public static final SiteConfig YANDERE = new SiteConfig("yande.re", "http://yande.re");

    private final String name;
    private final String rootUrl;

    public SiteConfig(String name, String rootUrl){
        this.name = name;
        this.rootUrl = rootUrl;
    }

    public String getName(){
        return name;
    }

    public String getRootUrl(){
        return rootUrl;
    }
}
