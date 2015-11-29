/*
 * Created 2015-11-28 21:51:19
 */
package io.github.azige.moebooruviewer;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Azige
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Post{

    private long id;
    @JsonProperty("preview_url")
    private String previewUrl;
    @JsonProperty("sample_url")
    private String sampleUrl;
    @JsonProperty("file_url")
    private String originUrl;
    private String tags;

    public Post(){
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        this.id = id;
    }

    public String getPreviewUrl(){
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl){
        this.previewUrl = previewUrl;
    }

    public String getSampleUrl(){
        return sampleUrl;
    }

    public void setSampleUrl(String sampleUrl){
        this.sampleUrl = sampleUrl;
    }

    public String getTags(){
        return tags;
    }

    public void setTags(String tags){
        this.tags = tags;
    }

    public String getOriginUrl(){
        return originUrl;
    }

    public void setOriginUrl(String originUrl){
        this.originUrl = originUrl;
    }

    @Override
    public int hashCode(){
        int hash = 7;
        hash = 37 * hash + (int)(this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        final Post other = (Post)obj;
        if (this.id != other.id){
            return false;
        }
        return true;
    }

    @Override
    public String toString(){
        return "Post{" + "id=" + id + ", previewUrl=" + previewUrl + '}';
    }
}
