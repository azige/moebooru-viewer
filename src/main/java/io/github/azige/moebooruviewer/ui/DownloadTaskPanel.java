/*
 * Copyright (C) 2016 Azige
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.azige.moebooruviewer.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import io.github.azige.moebooruviewer.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownloadTaskPanel extends javax.swing.JPanel {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadTaskPanel.class);

    File downloadFile;

    /**
     * Creates new form DownloadTaskPanel
     */
    public DownloadTaskPanel(){
        initComponents();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        taskNameLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        progressLabel = new javax.swing.JLabel();
        openButton = new javax.swing.JButton();
        browseButton = new javax.swing.JButton();
        retryButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEtchedBorder());

        taskNameLabel.setText("file name"); // NOI18N
        taskNameLabel.setMaximumSize(new java.awt.Dimension(100, 15));

        progressLabel.setText("null");
        progressLabel.setToolTipText("null");
        progressLabel.setPreferredSize(new java.awt.Dimension(50, 15));

        openButton.setText(Localization.getString("open")); // NOI18N
        openButton.setEnabled(false);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        browseButton.setText(Localization.getString("browse")); // NOI18N
        browseButton.setEnabled(false);
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        retryButton.setText(Localization.getString("retry")); // NOI18N
        retryButton.setEnabled(false);
        retryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retryButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(taskNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(progressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(openButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(retryButton))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(taskNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(openButton)
                    .addComponent(browseButton)
                    .addComponent(retryButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        try{
            Desktop.getDesktop().open(downloadFile);
        }catch (IOException ex){
            LOG.info("IO异常", ex); //NOI18N
        }
    }//GEN-LAST:event_openButtonActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        try{
            Desktop.getDesktop().browse(downloadFile.getParentFile().toURI());
        }catch (IOException ex){
            LOG.info("IO异常", ex); //NOI18N
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void retryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_retryButtonActionPerformed
        throw new UnsupportedOperationException();
//        retryButton.setEnabled(false);
//        executor.execute(task);
    }//GEN-LAST:event_retryButtonActionPerformed

    public void setTaskName(String fileName){
        final int textLengthLimit = 40;
        if (fileName.length() > textLengthLimit){
            taskNameLabel.setText(fileName.substring(0, textLengthLimit) + "..."); //NOI18N
        }else{
            taskNameLabel.setText(fileName);
        }
        taskNameLabel.setToolTipText(fileName);
    }

    public void setDownloadFile(File downloadFile){
        this.downloadFile = downloadFile;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JButton openButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JButton retryButton;
    private javax.swing.JLabel taskNameLabel;
    // End of variables declaration//GEN-END:variables

    public void onProgress(double rate){
        int percent = (int)(rate * 100);
        String text = "" + percent + "%"; //NOI18N
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percent);
            progressLabel.setText(text);
        });
    }

    public void onComplete(File file){
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            progressLabel.setText(Localization.getString("done"));
            openButton.setEnabled(true);
            browseButton.setEnabled(true);
        });
    }

    public void onFail(Throwable ex){
        SwingUtilities.invokeLater(() -> {
            progressLabel.setText(Localization.getString("failed"));
            retryButton.setEnabled(true);
        });
    }
}
