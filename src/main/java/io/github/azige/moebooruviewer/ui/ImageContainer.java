/*
 * Created 2015-11-29 14:29:53
 */
package io.github.azige.moebooruviewer.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

/**
 *
 * @author Azige
 */
public class ImageContainer extends JComponent{

    private String text;
    private Image image;

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        if (text != null){
            Rectangle2D bounds = g2.getFontMetrics().getStringBounds(text, g2);
            int x = getWidth() / 2 - (int)bounds.getWidth() / 2;
            int y = getHeight() / 2 - (int)bounds.getHeight() / 2;
            g2.drawString(text, x, y);
        }
        if (image != null){
            int x = getWidth() / 2 - image.getWidth(null) / 2;
            int y = getHeight() / 2 - image.getHeight(null) / 2;
            g2.drawImage(image, x, y, null);
        }
    }

    public String getText(){
        return text;
    }

    public void setText(String text){
        this.text = text;
        this.image = null;
    }

    public Image getImage(){
        return image;
    }

    public void setImage(Image image){
        this.image = image;
        this.text = null;
    }
}
