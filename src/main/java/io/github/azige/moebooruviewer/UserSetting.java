/*
 * Created 2015-12-4 1:27:29
 */
package io.github.azige.moebooruviewer;

import java.io.File;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 * @author Azige
 */
public class UserSetting{

    private static final int MAX_HISTORY_COUNT = 10;

    private SiteConfig siteConfig;
    private LinkedList<String> searchHistories = new LinkedList<>();
    private File lastSaveDir;

    public SiteConfig getSiteConfig(){
        return siteConfig;
    }

    public void setSiteConfig(SiteConfig siteConfig){
        this.siteConfig = siteConfig;
    }

    @XmlElementWrapper(name = "searchHistories")
    @XmlElement(name = "history")
    public LinkedList<String> getSearchHistories(){
        return searchHistories;
    }

    public void setSearchHistories(LinkedList<String> searchHistories){
        this.searchHistories = searchHistories;
    }

    public void addSearchHistory(String keywords){
        searchHistories.offer(keywords);
        if (searchHistories.size() > MAX_HISTORY_COUNT){
            searchHistories.poll();
        }
    }

    public File getLastSaveDir(){
        return lastSaveDir;
    }

    public void setLastSaveDir(File lastSaveDir){
        this.lastSaveDir = lastSaveDir;
    }
}
