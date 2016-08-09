/*
 * Created 2015-11-27 23:32:57
 */
package io.github.azige.moebooruviewer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ListPostFrame extends javax.swing.JFrame{

    private static final Logger logger = LoggerFactory.getLogger(ListPostFrame.class);
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 100;
    private static final int PREVIEW_BORDER_THICKNESS = 3;
    private static final Color CLICKED_POST_BORDER_COLOR = Color.YELLOW;
    private static final Color CACHED_POST_BORDER_COLOR = Color.GREEN;

    @Autowired
    private ApplicationContext context;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private MoebooruViewer moebooruViewer;
    @Autowired
    private NetIO netIO;
    @Autowired
    private ExecutorService executor;
    @Autowired
    private MoebooruAPI mapi;
    @Autowired
    private UserSetting userSetting;
    @Autowired
    private DownloadManagerFrame downloadManagerFrame;

    private int pageCount = 1;
    private Set<Post> posts = new HashSet<>();
    private final JLabel loadMoreLabel;
    private String[] tags = {};
    private int pageSize;
    private Post showingPopupMenuPost;
    private JLabel showingPopupMenuLabel;

    /**
     * Creates new form MainFrame
     */
    public ListPostFrame(){
        initComponents();

        setLocationRelativeTo(null);
        postsPanel.setLayout(new FlowLayout(){

            @Override
            public void layoutContainer(Container target){

                int columnCount = target.getSize().width / (PREVIEW_WIDTH + getHgap());
                int rowCount = posts.size() / columnCount + 1;
                target.setPreferredSize(new Dimension(1,
                    rowCount * (PREVIEW_HEIGHT + getVgap()) + getVgap()
                ));
                super.layoutContainer(target);
            }

        });
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        loadMoreLabel = new JLabel(Localization.getString("load_more"));
        loadMoreLabel.setForeground(Color.WHITE);
        loadMoreLabel.setHorizontalAlignment(JLabel.CENTER);
        loadMoreLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        loadMoreLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadMoreLabel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e){
                if (loadMoreLabel.isEnabled()){
                    loadImages();
                }
            }

        });

        postsPanel.add(loadMoreLabel);
    }

    @PostConstruct
    private void init(){
        setTitle(siteConfig.getName() + " Viewer");
        pageSize = userSetting.getPageSize();
        try{
            setIconImage(siteConfig.getIcon());
        }catch (IOException ex){
            logger.info("无法设置图标", ex);
        }
    }

    public String[] getTags(){
        return tags;
    }

    public void setTags(String[] tags){
        this.tags = Objects.requireNonNull(tags);
        refreshTitle();
    }

    @Override
    protected void processWindowEvent(WindowEvent e){
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING || e.getID() == WindowEvent.WINDOW_CLOSED){
        }
    }

    private void refreshTitle(){
        StringBuilder sb = new StringBuilder();
        sb.append(siteConfig.getName()).append(" Viewer");
        sb.append(' ').append(Localization.format("page_is", pageCount));
        if (tags.length > 0){
            sb.append(" ").append(Localization.format("tag_is", Stream.of(tags).reduce((a, b) -> a + " " + b).get()));
        }

        setTitle(sb.toString());
    }

    public void loadImages(){
        loadMoreLabel.setText(Localization.getString("loading"));
        loadMoreLabel.setEnabled(false);
        refreshTitle();
        executor.execute(() -> {
            List<Post> postList = netIO.retry(() -> mapi.listPosts(pageCount, pageSize, tags));
            SwingUtilities.invokeLater(() -> {
                pageCount++;
                postsPanel.remove(loadMoreLabel);
                for (Post post : postList){
                    if (posts.contains(post)){
                        continue;
                    }
                    posts.add(post);
                    JLabel label = new JLabel(Localization.getString("loading"));
                    label.setForeground(Color.WHITE);
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    label.setToolTipText(String.valueOf(post.getId()));
                    if (netIO.getSampleFile(post).exists()){
                        label.setBorder(new LineBorder(CACHED_POST_BORDER_COLOR, PREVIEW_BORDER_THICKNESS));
                    }

                    // TODO: 整理这段代码
                    class LoadImageTask implements Runnable{

                        boolean force = false;

                        @Override
                        public void run(){
                            Image image = netIO.loadPreview(post, force);
                            SwingUtilities.invokeLater(() -> {
                                if (image != null){
                                    label.setText("");
                                    Dimension size = label.getPreferredSize();
                                    label.setIcon(new ImageIcon(Utils.resizeImage(image, size.getWidth(), size.getHeight())));
                                }else{
                                    label.setText(Localization.getString("unable_to_load"));
                                }
                            });
                        }

                    }

                    label.addMouseListener(new MouseAdapter(){

                        @Override
                        public void mouseClicked(MouseEvent e){
                            if (SwingUtilities.isLeftMouseButton(e)){
                                moebooruViewer.showPost(post);
                                label.setBorder(new LineBorder(CLICKED_POST_BORDER_COLOR, PREVIEW_BORDER_THICKNESS));
                            }
                        }

                        @Override
                        public void mousePressed(MouseEvent e){
                            popupMenu(e);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e){
                            popupMenu(e);
                        }

                        private void popupMenu(MouseEvent e){
                            if (e.isPopupTrigger()){
                                showingPopupMenuLabel = label;
                                showingPopupMenuPost = post;
                                postPreviewPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    });
                    postsPanel.add(label);
                    LoadImageTask task = new LoadImageTask();
                    executor.execute(task);
                }
                postsPanel.add(loadMoreLabel);
                if (!postList.isEmpty()){
                    loadMoreLabel.setText(Localization.getString("load_more"));
                    loadMoreLabel.setEnabled(true);
                }else{
                    loadMoreLabel.setText(Localization.getString("no_more_items"));
                }
            });
        });
    }

    public void clear(){
        posts.clear();
        postsPanel.removeAll();
        postsPanel.add(loadMoreLabel);
        repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        postPreviewPopupMenu = new javax.swing.JPopupMenu();
        reloadMenuItem = new javax.swing.JMenuItem();
        scrollPane = new javax.swing.JScrollPane();
        postsPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openPostMenuItem = new javax.swing.JMenuItem();
        jumpPageMenuItem = new javax.swing.JMenuItem();
        searchTagMenuItem = new javax.swing.JMenuItem();
        favoriteTagMenu = new javax.swing.JMenu();
        favoriteTagSeparator = new javax.swing.JPopupMenu.Separator();
        addFavoriteTagMenuItem = new javax.swing.JMenuItem();
        removeFavoriteTagMenuItem = new javax.swing.JMenuItem();
        searchHistoryMenu = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        switchKonachanMenuItem = new javax.swing.JMenuItem();
        switchYandereMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        configMenuItem = new javax.swing.JMenuItem();
        cleanCacheMenuItem = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        showDownloadManagerMenuItem = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        showVersionMenuItem = new javax.swing.JMenuItem();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("io/github/azige/moebooruviewer/Messages"); // NOI18N
        reloadMenuItem.setText(bundle.getString("reload")); // NOI18N
        reloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadMenuItemActionPerformed(evt);
            }
        });
        postPreviewPopupMenu.add(reloadMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Moebooru Viewer");
        setMinimumSize(new java.awt.Dimension(850, 650));

        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        postsPanel.setBackground(new java.awt.Color(34, 34, 34));
        postsPanel.setPreferredSize(new java.awt.Dimension(800, 10000));
        postsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        scrollPane.setViewportView(postsPanel);

        jMenu1.setText(bundle.getString("functions")); // NOI18N
        jMenu1.setMinimumSize(new java.awt.Dimension(200, 0));

        openPostMenuItem.setText(Localization.getString("retrieve_post_by_id")); // NOI18N
        openPostMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPostMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openPostMenuItem);

        jumpPageMenuItem.setText(bundle.getString("jump_to_page")); // NOI18N
        jumpPageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jumpPageMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(jumpPageMenuItem);

        searchTagMenuItem.setText(bundle.getString("search_by_tag")); // NOI18N
        searchTagMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchTagMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(searchTagMenuItem);

        favoriteTagMenu.setText(Localization.getString("favorite_tags")); // NOI18N
        favoriteTagMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                favoriteTagMenuMouseEntered(evt);
            }
        });
        favoriteTagMenu.add(favoriteTagSeparator);

        addFavoriteTagMenuItem.setText(Localization.getString("add")); // NOI18N
        addFavoriteTagMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFavoriteTagMenuItemActionPerformed(evt);
            }
        });
        favoriteTagMenu.add(addFavoriteTagMenuItem);

        removeFavoriteTagMenuItem.setText(Localization.getString("remove")); // NOI18N
        removeFavoriteTagMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFavoriteTagMenuItemActionPerformed(evt);
            }
        });
        favoriteTagMenu.add(removeFavoriteTagMenuItem);

        jMenu1.add(favoriteTagMenu);

        searchHistoryMenu.setText(bundle.getString("search_history")); // NOI18N
        searchHistoryMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchHistoryMenuMouseEntered(evt);
            }
        });
        jMenu1.add(searchHistoryMenu);

        jMenu2.setText(bundle.getString("switch_site")); // NOI18N

        switchKonachanMenuItem.setText("Konachan.com");
        switchKonachanMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                switchKonachanMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(switchKonachanMenuItem);

        switchYandereMenuItem.setText("yande.re");
        switchYandereMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                switchYandereMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(switchYandereMenuItem);

        jMenu1.add(jMenu2);
        jMenu1.add(jSeparator1);

        exitMenuItem.setText(bundle.getString("exit")); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu4.setText(bundle.getString("settings")); // NOI18N

        configMenuItem.setText(bundle.getString("settings")); // NOI18N
        configMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configMenuItemActionPerformed(evt);
            }
        });
        jMenu4.add(configMenuItem);

        cleanCacheMenuItem.setText(bundle.getString("clear_cache")); // NOI18N
        cleanCacheMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cleanCacheMenuItemActionPerformed(evt);
            }
        });
        jMenu4.add(cleanCacheMenuItem);

        jMenuBar1.add(jMenu4);

        jMenu5.setText(Localization.getString("view")); // NOI18N

        showDownloadManagerMenuItem.setText(Localization.getString("download_manager")); // NOI18N
        showDownloadManagerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDownloadManagerMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(showDownloadManagerMenuItem);

        jMenuBar1.add(jMenu5);

        jMenu3.setText(bundle.getString("help")); // NOI18N

        showVersionMenuItem.setText(bundle.getString("version")); // NOI18N
        showVersionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showVersionMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(showVersionMenuItem);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 850, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 629, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchTagMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchTagMenuItemActionPerformed
        String keywords = JOptionPane.showInputDialog(this, Localization.getString("enter_search_tags"),
            Localization.getString("search_by_tag"), JOptionPane.PLAIN_MESSAGE);
        if (keywords != null){
            moebooruViewer.searchByTags(keywords);
        }
    }//GEN-LAST:event_searchTagMenuItemActionPerformed

    private void switchKonachanMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_switchKonachanMenuItemActionPerformed
        moebooruViewer.switchSite(SiteConfig.KONACHAN);
    }//GEN-LAST:event_switchKonachanMenuItemActionPerformed

    private void switchYandereMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_switchYandereMenuItemActionPerformed
        moebooruViewer.switchSite(SiteConfig.YANDERE);
    }//GEN-LAST:event_switchYandereMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        moebooruViewer.exit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void jumpPageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jumpPageMenuItemActionPerformed
        String pageString = JOptionPane.showInputDialog(this, Localization.getString("enter_page_number"),
            Localization.getString("jump_to_page"), JOptionPane.PLAIN_MESSAGE);
        if (pageString != null){
            pageCount = Integer.parseInt(pageString);
            clear();
            loadImages();
        }
    }//GEN-LAST:event_jumpPageMenuItemActionPerformed

    private void searchHistoryMenuMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchHistoryMenuMouseEntered
        searchHistoryMenu.removeAll();
        userSetting.getSearchHistories().forEach(keywords -> {
            JMenuItem menuItem = new JMenuItem(keywords);
            menuItem.addActionListener(event -> moebooruViewer.searchByTags(keywords));
            searchHistoryMenu.add(menuItem);
        });
    }//GEN-LAST:event_searchHistoryMenuMouseEntered

    private void showVersionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showVersionMenuItemActionPerformed
        String version = Localization.getString("version_stamp");
        JOptionPane.showMessageDialog(this, version, Localization.getString("version"), JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_showVersionMenuItemActionPerformed

    private void openPostMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPostMenuItemActionPerformed
        String id = JOptionPane.showInputDialog(this, Localization.getString("enter_id"),
            Localization.getString("retrieve_post_by_id"), JOptionPane.PLAIN_MESSAGE);
        if (id != null){
            try{
                moebooruViewer.showPostById(Integer.parseInt(id), this);
            }catch (NumberFormatException ex){
                JOptionPane.showMessageDialog(null, Localization.getString("id_format_is_incorrect"),
                    Localization.getString("error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_openPostMenuItemActionPerformed

    private void configMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configMenuItemActionPerformed
        ConfigDialog configDialog = context.getBean(ConfigDialog.class);
        configDialog.setLocationRelativeTo(this);
        configDialog.setVisible(true);
    }//GEN-LAST:event_configMenuItemActionPerformed

    private void cleanCacheMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cleanCacheMenuItemActionPerformed
        if (netIO.cleanCache()){
            JOptionPane.showMessageDialog(this, Localization.getString("successfully_deleted"),
                Localization.getString("success"), JOptionPane.INFORMATION_MESSAGE);
        }else{
            JOptionPane.showMessageDialog(this, Localization.getString("failed_to_delete"),
                Localization.getString("error"), JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_cleanCacheMenuItemActionPerformed

    private void reloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadMenuItemActionPerformed
        showingPopupMenuLabel.setIcon(null);
        showingPopupMenuLabel.setText(Localization.getString("loading"));

        JLabel label = showingPopupMenuLabel;
        Post post = showingPopupMenuPost;
        executor.execute(() -> {
            Image image = netIO.loadPreview(post, true);
            SwingUtilities.invokeLater(() -> {
                if (image != null){
                    label.setText("");
                    Dimension size = label.getPreferredSize();
                    label.setIcon(new ImageIcon(Utils.resizeImage(image, size.getWidth(), size.getHeight())));
                }else{
                    label.setText(Localization.getString("unable_to_load"));
                }
            });
        });
    }//GEN-LAST:event_reloadMenuItemActionPerformed

    private void showDownloadManagerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDownloadManagerMenuItemActionPerformed
        downloadManagerFrame.setVisible(true);
    }//GEN-LAST:event_showDownloadManagerMenuItemActionPerformed

    private void addFavoriteTagMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFavoriteTagMenuItemActionPerformed
        String tag = JOptionPane.showInputDialog(this, Localization.getString("enter_favorite_tag_add"),
            Localization.getString("favorite_tags"), JOptionPane.PLAIN_MESSAGE);
        if (tag != null){
            userSetting.getFavoriteTags().add(tag);
        }
    }//GEN-LAST:event_addFavoriteTagMenuItemActionPerformed

    private void removeFavoriteTagMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFavoriteTagMenuItemActionPerformed
        String tag = JOptionPane.showInputDialog(this, Localization.getString("enter_favorite_tag_remove"),
            Localization.getString("favorite_tags"), JOptionPane.PLAIN_MESSAGE);
        if (tag != null){
            userSetting.getFavoriteTags().remove(tag);
        }
    }//GEN-LAST:event_removeFavoriteTagMenuItemActionPerformed

    private void favoriteTagMenuMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_favoriteTagMenuMouseEntered
        favoriteTagMenu.removeAll();
        userSetting.getFavoriteTags().forEach(keywords -> {
            JMenuItem menuItem = new JMenuItem(keywords);
            menuItem.addActionListener(event -> moebooruViewer.searchByTags(keywords));
            favoriteTagMenu.add(menuItem);
        });
        favoriteTagMenu.add(favoriteTagSeparator);
        favoriteTagMenu.add(addFavoriteTagMenuItem);
        favoriteTagMenu.add(removeFavoriteTagMenuItem);
    }//GEN-LAST:event_favoriteTagMenuMouseEntered

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem addFavoriteTagMenuItem;
    private javax.swing.JMenuItem cleanCacheMenuItem;
    private javax.swing.JMenuItem configMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu favoriteTagMenu;
    private javax.swing.JPopupMenu.Separator favoriteTagSeparator;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JMenuItem jumpPageMenuItem;
    private javax.swing.JMenuItem openPostMenuItem;
    private javax.swing.JPopupMenu postPreviewPopupMenu;
    private javax.swing.JPanel postsPanel;
    private javax.swing.JMenuItem reloadMenuItem;
    private javax.swing.JMenuItem removeFavoriteTagMenuItem;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JMenu searchHistoryMenu;
    private javax.swing.JMenuItem searchTagMenuItem;
    private javax.swing.JMenuItem showDownloadManagerMenuItem;
    private javax.swing.JMenuItem showVersionMenuItem;
    private javax.swing.JMenuItem switchKonachanMenuItem;
    private javax.swing.JMenuItem switchYandereMenuItem;
    // End of variables declaration//GEN-END:variables
}
