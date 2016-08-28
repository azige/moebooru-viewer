/*
 * Created 2015-12-4 1:27:29
 */
package io.github.azige.moebooruviewer;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 * @author Azige
 */
public class UserSetting{

    private static final int MAX_HISTORY_COUNT = 10;
    private static final int SAVE_LOCATION_ITEMS_COUNT = 4;

    private SiteConfig siteConfig;
    private LinkedList<String> searchHistories = new LinkedList<>();
    private LinkedHashSet<String> favoriteTags = new LinkedHashSet<>();
    private File lastSaveDir;
    private boolean safeMode = true;
    /**
     * Save the class name of L&F instead of the name of L&F.
     * Since the names in LookAndFeelInfo and LookAndFeel may not match.
     */
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
            new SaveLocation(Localization.getString("wallpaper"), new File("wallpaper").getAbsoluteFile()),
            new SaveLocation(Localization.getString("collections"), new File("collections").getAbsoluteFile()),
            new SaveLocation(Localization.getString("private_collection"), new File(".collections").getAbsoluteFile()),
            new SaveLocation(Localization.getString("desktop"), FileSystemView.getFileSystemView().getHomeDirectory())
        );
        setting.siteConfig = SiteConfig.KONACHAN;
        setting.pageSize = 20;
        return setting;
    }

    public void verifyAndRepair(){
        UserSetting defaulSetting = createDefaultSetting();
        if (saveLocations.size() < SAVE_LOCATION_ITEMS_COUNT){
            saveLocations = defaulSetting.saveLocations;
        }
        if (siteConfig == null){
            siteConfig = defaulSetting.siteConfig;
        }
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
        if (keywords.equals("")){
            return;
        }
        if (searchHistories.contains(keywords)){
            searchHistories.remove(keywords);
        }
        searchHistories.addFirst(keywords);
        if (searchHistories.size() > MAX_HISTORY_COUNT){
            searchHistories.removeLast();
        }
    }

    @XmlElementWrapper(name = "favoriteTags")
    @XmlElement(name = "tag")
    public LinkedHashSet<String> getFavoriteTags(){
        return favoriteTags;
    }

    public void setFavoriteTags(LinkedHashSet<String> favoriteTags){
        this.favoriteTags = favoriteTags;
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

    // Cause info in old files lost.
    // TODO: decomment this at v1.0
    //@XmlElementWrapper(name = "saveLocations")
    //@XmlElement(name = "saveLocation")
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
