var exec = require('cordova/exec');

exports.deviceControl = function (uid, token, mac, key, value, success, error) {
    exec(success, error, "gwsdkDevice", "deviceControl", [uid, token, mac, key, value]);
};


