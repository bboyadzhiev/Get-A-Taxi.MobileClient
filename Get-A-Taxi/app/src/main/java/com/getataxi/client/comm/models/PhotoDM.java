package com.getataxi.client.comm.models;

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
    public byte[] content;

    @SerializedName("extension")
    public String extension;
}
