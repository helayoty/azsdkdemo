package azsdkdemo.java.lib;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Image implements Serializable {

    public enum ImageType {
        NEW,
        MEME
    }

    @JsonProperty("id")
    private UUID id;
    @JsonProperty("uid")
    private String uid;
    @JsonProperty("title")
    private String title;
    @JsonProperty("url")
    private String url;
    @JsonProperty("extension")
    private String extension;
    @JsonProperty("blobName")
    private String blobName;
    @JsonProperty("blob")
    private String blobUri;
    @JsonProperty("text")
    private String text;
    @JsonProperty("sentiment")
    private String sentiment;
    @JsonProperty("status")
    private String status;
    @JsonProperty("createdDate")
    private Date createdDate;

    public ImageType type = ImageType.NEW;


    public void setSentiment(String sentiment){
        this.sentiment = sentiment;
    }

    public String getUid() {
        return this.id.toString();
    }

    public String getTitle() {
        return this.title;
    }
    public String getUrl() {
        return this.url;
    }

    public String getBlobUri() {
        return this.blobUri;
    }

    public String getText()
    {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSentiment(){
        return this.sentiment;
    }


    public String getBlobName (){
        return this.extension.concat(this.id.toString());
    }

    public String getStyle () {
        String style = "";
        switch (this.sentiment.toLowerCase()){
            case "positive":
                style = "success";
                break;
            case "negative":
                style = "danger";
                break;
            case "neutral":
                style = "dark";
                break;
            case "mixed":
                style = "waring";
                break;
            case "loading":
                style = "white";
                break;
        }
        return  style;
    }
}
