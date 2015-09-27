package com.getataxi.client.comm.models;

import android.util.Base64;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by Bobi on 8/12/2015.
 */
@Parcel
public class PhotoDM {
    @SerializedName("photoId")
    public int photoId;

    @SerializedName("content")
    public String content;

    @SerializedName("fileExtension")
    public String extension;

    public byte[] getImage() {
            return Base64.decode(this.content, Base64.DEFAULT);
    }
}
