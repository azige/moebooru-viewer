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

    private String name;
    private String rootUrl;

    public SiteConfig(){
    }

    public SiteConfig(String name, String rootUrl){
        this.name = name;
        this.rootUrl = rootUrl;
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
}
