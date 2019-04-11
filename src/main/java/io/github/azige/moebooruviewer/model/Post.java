/*
 * Created 2015-11-28 21:51:19
 */
package io.github.azige.moebooruviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Azige
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {

    private int id;
    @JsonProperty("preview_url")
    private String previewUrl;
    @JsonProperty("sample_url")
    private String sampleUrl;
    @JsonProperty("file_url")
    private String originUrl;
    @JsonProperty("jpeg_url")
    private String jpegUrl;
    private String tags;
    private String source;
    @JsonProperty("parent_id")
    private Integer parentId;
    @JsonProperty("has_children")
    private boolean hasChildren;

    @JsonIgnore
    private Pool pool;

    public Post() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getSampleUrl() {
        return sampleUrl;
    }

    public void setSampleUrl(String sampleUrl) {
        this.sampleUrl = sampleUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getJpegUrl() {
        return jpegUrl;
    }

    public void setJpegUrl(String jpegUrl) {
        this.jpegUrl = jpegUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Pool getPool() {
        return pool;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Post other = (Post) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Post{" + "id=" + id + ", previewUrl=" + previewUrl + '}';
    }
}
