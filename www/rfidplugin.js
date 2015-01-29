var RFIDPlugin = function() {};

RFIDPlugin.prototype.doscan = function(action,args,success, fail) {
    cordova.exec(success, fail, "RFIDPlugin",action, args);
};

var RFIDPlugin = new RFIDPlugin();
module.exports = RFIDPlugin;