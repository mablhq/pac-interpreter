// Source: https://www.websense.com/content/support/library/web/v76/pac_file_best_practices/PAC_file_sample.aspx

function FindProxyForURL(url, host) {
    if (isInNet(myIpAddress(), "1.1.0.0", "255.0.0.0")) {
        return "PROXY wcg1.example.com:8080; " + "PROXY wcg2.example.com:8080";
    }

    if (isInNet(myIpAddress(), "1.2.0.0", "255.0.0.0")) {
        return "PROXY wcg1.example.com:8080; " + "PROXY wcg2.example.com:8080";
    }

    if (isInNet(myIpAddress(), "1.3.0.0", "255.0.0.0")) {
        return "PROXY wcg2.example.com:8080; " + "PROXY wcg1.example.com:8080";
    }

    if (isInNet(myIpAddress(), "1.4.0.0", "255.0.0.0")) {
        return "PROXY wcg2.example.com:8080; " + "PROXY wcg1.example.com:8080";
    }
    else return "DIRECT";
}
