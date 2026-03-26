package com.qiplat.sweeteditor.core.visual;

import com.google.gson.annotations.SerializedName;

public class ScrollbarModel {
    @SerializedName("visible") public boolean visible;
    @SerializedName("alpha") public float alpha;
    @SerializedName("thumb_active") public boolean thumbActive;
    @SerializedName("track") public ScrollbarRect track;
    @SerializedName("thumb") public ScrollbarRect thumb;
}
