package org.opencv.admin.bean;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * 作者：Zhou on 2019/3/4 15:55
 * 简介：正面身份证
 */
public class IDCardBean implements Parcelable{

    public int state;
    public String name;
    public String sex;
    public String ethnic;
    public String birthData;
    public String address;
    public String idNumber;
    public String validity;
    public String issuance;
    public Bitmap bmp;

    public IDCardBean() {
    }


    protected IDCardBean(Parcel in) {
        state = in.readInt();
        name = in.readString();
        sex = in.readString();
        ethnic = in.readString();
        birthData = in.readString();
        address = in.readString();
        idNumber = in.readString();
        validity = in.readString();
        issuance = in.readString();
        bmp = in.readParcelable(Bitmap.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(state);
        dest.writeString(name);
        dest.writeString(sex);
        dest.writeString(ethnic);
        dest.writeString(birthData);
        dest.writeString(address);
        dest.writeString(idNumber);
        dest.writeString(validity);
        dest.writeString(issuance);
        dest.writeParcelable(bmp, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<IDCardBean> CREATOR = new Creator<IDCardBean>() {
        @Override
        public IDCardBean createFromParcel(Parcel in) {
            return new IDCardBean(in);
        }

        @Override
        public IDCardBean[] newArray(int size) {
            return new IDCardBean[size];
        }
    };

    public boolean isFrontSuccess() {
        boolean state = true;
        if (TextUtils.isEmpty(name)) {
            state = false;
        }
        if (TextUtils.isEmpty(sex)) {
            state = false;
        }
        if (TextUtils.isEmpty(ethnic)) {
            state = false;
        }
        if (TextUtils.isEmpty(birthData)) {
            state = false;
        }
        if (TextUtils.isEmpty(address)) {
            state = false;
        }
        if (TextUtils.isEmpty(idNumber)) {
            state = false;
        }

        return state;
    }

    public boolean isBackSuccess() {
        boolean state = true;
        if (TextUtils.isEmpty(validity)) {
            state = false;
        }
        if (TextUtils.isEmpty(issuance)) {
            state = false;
        }

        return state;
    }


    @Override
    public String toString() {
        return "IDCardBean{" +
                "state=" + state +
                ", name='" + name + '\'' +
                ", sex='" + sex + '\'' +
                ", ethnic='" + ethnic + '\'' +
                ", birthData='" + birthData + '\'' +
                ", address='" + address + '\'' +
                ", idNumber='" + idNumber + '\'' +
                ", validity='" + validity + '\'' +
                ", issuance='" + issuance + '\'' +
                ", bmp=" + bmp +
                '}';
    }
}
