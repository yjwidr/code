package com.xxx.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

public class PackageVersionParameter {
    @NotBlank(message ="packageVersion cannot be empty")
    @Length(min=1, max=128, message="packageVersion length must be between 1-128")
//    @Pattern(regexp = "^[A-Za-z]+[\\s\\S]*+$", message = "the packageVersion must begin with an alphabetic or underscore")
    private String packageVersion;
    @Min(value=1,message ="summaryDataLength Must be greater than 1")
    private int summaryDataLength;
    @Min(value=1,message ="listDataLength Must be greater than 1")
    private int listDataLength;
    @NotBlank(message ="base64Data cannot be empty")
    private String base64Data;
    public String getPackageVersion() {
        return packageVersion;
    }
    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }
    public int getSummaryDataLength() {
        return summaryDataLength;
    }
    public void setSummaryDataLength(int summaryDataLength) {
        this.summaryDataLength = summaryDataLength;
    }
    public int getListDataLength() {
        return listDataLength;
    }
    public void setListDataLength(int listDataLength) {
        this.listDataLength = listDataLength;
    }
    public String getBase64Data() {
        return base64Data;
    }
    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }


}
