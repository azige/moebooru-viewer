/*
 * Created 2016-6-19 23:19:39
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 *
 * @author Azige
 */
public class ImageSelection implements Transferable{

    private Image image;

    public ImageSelection(Image image){
        this.image = image;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors(){
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor){
        return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException{
        if (isDataFlavorSupported(flavor)){
            return image;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
