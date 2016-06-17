/*
 * Created 2015-12-4 1:27:29
 */
package io.github.azige.moebooruviewer;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
    private boolean safeMode = true;
    private String LookAndFeel;
    private List<SaveLocation> saveLocations;
    private int pageSize;

    public static class SaveLocation{

        private String name;
        private File location;

        public SaveLocation(){
        }

        public SaveLocation(String name, File location){
            this.name = name;
            this.location = location;
        }

        public String getName(){
            return name;
        }

        public void setName(String name){
            this.name = name;
        }

        public File getLocation(){
            return location;
        }

        public void setLocation(File location){
            this.location = location;
        }
    }

    public static UserSetting createDefaultSetting(){
        UserSetting setting = new UserSetting();
        setting.saveLocations = Arrays.asList(
            new SaveLocation("壁纸", new File("wallpaper").getAbsoluteFile()),
            new SaveLocation("可公开收藏", new File("collections").getAbsoluteFile()),
            new SaveLocation("非公开收藏", new File(".collections").getAbsoluteFile()),
            new SaveLocation("暂存", new File("recycler").getAbsoluteFile())
        );
        setting.siteConfig = SiteConfig.KONACHAN;
        setting.pageSize = 20;
        return setting;
    }

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

    public boolean isSafeMode(){
        return safeMode;
    }

    public void setSafeMode(boolean safeMode){
        this.safeMode = safeMode;
    }

    public String getLookAndFeel(){
        return LookAndFeel;
    }

    public void setLookAndFeel(String LookAndFeel){
        this.LookAndFeel = LookAndFeel;
    }

    public List<SaveLocation> getSaveLocations(){
        return saveLocations;
    }

    public void setSaveLocations(List<SaveLocation> saveLocations){
        this.saveLocations = saveLocations;
    }

    public int getPageSize(){
        return pageSize;
    }

    public void setPageSize(int pageSize){
        this.pageSize = pageSize;
    }
}
