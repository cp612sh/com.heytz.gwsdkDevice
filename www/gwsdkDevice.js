var exec = require('cordova/exec');

exports.deviceControl = function ( appid,productKey,uid,token,mac,value , success, error) {
    exec(success, error, "gwsdkDevice", "deviceControl", [appid,productKey,uid,token,mac,value]);
};


