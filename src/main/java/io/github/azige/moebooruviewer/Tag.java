/*
 * Created 2015-11-29 14:03:24
 */
package io.github.azige.moebooruviewer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Azige
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag{

    public static final int TYPE_GENERAL = 0;
    public static final int TYPE_ARTIST = 1;
    public static final int TYPE_COPYRIGHT = 3;
    public static final int TYPE_CHARACTER = 4;

    private String name;
    private int type;

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public int getType(){
        return type;
    }

    public void setType(int type){
        this.type = type;
    }
}
