package com.android.gmacs.album;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class ImageUrlArrayListWrapper implements Parcelable {

    public static final Creator CREATOR = new Creator<ImageUrlArrayListWrapper>() {
        @Override
        public ImageUrlArrayListWrapper createFromParcel(Parcel source) {
            return new ImageUrlArrayListWrapper(source);
        }

        @Override
        public ImageUrlArrayListWrapper[] newArray(int size) {
            return new ImageUrlArrayListWrapper[size];
        }
    };
    public ArrayList<String> mList = new ArrayList<>();

    public ImageUrlArrayListWrapper(ArrayList<String> arrayList) {
        mList.addAll(arrayList);
    }

    private ImageUrlArrayListWrapper(Parcel source) {
        source.readStringList(mList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(mList);
    }

}
