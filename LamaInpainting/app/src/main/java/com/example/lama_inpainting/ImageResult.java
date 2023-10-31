package com.example.lama_inpainting;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ImageResult {

    @SerializedName("format")
    @Expose
    private String format;
    @SerializedName("img")
    @Expose
    private String img;
    @SerializedName("msg")
    @Expose
    private String msg;

    @SerializedName("size")
    @Expose
    private ArrayList<Integer> size;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getImg(){
        return img;
    }

    public void setImg(String img){
        this.img = img;
    }

    public String getMsg(){
        return msg;
    }

    public void setMsg(String msg){
        this.msg = msg;
    }

    public ArrayList<Integer> getSize(){
        return  size;
    }

    public void setSize(ArrayList<Integer> size){
        this.size = size;
    }
}
